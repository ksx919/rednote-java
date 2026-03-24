package com.rednote.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rednote.common.CursorResult;
import com.rednote.common.UserContext;
import com.rednote.entity.Post;
import com.rednote.entity.PostCollect;
import com.rednote.entity.PostLike;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.User;
import com.rednote.entity.vo.PostDetailVO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.mapper.PostCollectMapper;
import com.rednote.mapper.PostLikeMapper;
import com.rednote.mapper.PostMapper;
import com.rednote.mapper.UserMapper;
import com.rednote.recommend.dto.RecommendationFeedResponse;
import com.rednote.service.PostService;
import com.rednote.service.RecommendationService;
import com.rednote.service.UserService;
import com.rednote.utils.AliOssUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private PostLikeMapper postLikeMapper;

    @Resource
    private PostCollectMapper postCollectMapper;

    @Resource
    private RecommendationService recommendationService;

    @Override
    public PostDetailVO publishPost(PostPublishDTO postPublishDTO, MultipartFile[] files) {
        Post post = new Post();
        post.setTitle(postPublishDTO.getTitle());
        post.setContent(postPublishDTO.getContent());
        post.setUserId(UserContext.getUserId());
        post.setImages(aliOssUtil.uploadImages(files));
        post.setImgHeight(postPublishDTO.getImgHeight());
        post.setImgWidth(postPublishDTO.getImgWidth());
        if (!save(post)) {
            throw new RuntimeException("发布失败");
        }
        return BeanUtil.copyProperties(post, PostDetailVO.class);
    }

    @Override
    public CursorResult<PostInfoVO> getFeedList(Long lastId, int size) {
        // 1. 构造查询条件
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();

        // 核心逻辑：ID 倒序 (最新的在上面)
        query.orderByDesc(Post::getId);

        // 如果传了 lastId，说明是加载更多，要查 ID 比这个小的
        // SQL: WHERE id < {lastId}
        if (lastId != null) {
            query.lt(Post::getId, lastId);
        }

        // 限制条数：为了判断“是否还有更多”，我们故意多查 1 条
        query.last("LIMIT " + (size + 1));

        // 2. 执行查询
        List<Post> posts = list(query);

        // 3. 处理游标和 hasMore
        boolean hasMore = false;
        Long nextCursor = null;

        if (posts.size() > size) {
            hasMore = true;
            // 把多查的那一条删掉，不返回给前端
            posts.removeLast();
            // 下一次的游标，就是当前列表最后一条的 ID
            nextCursor = posts.getLast().getId();
        } else if (!posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        List<PostInfoVO> voList = convertToPostInfoVO(posts);

        return CursorResult.build(voList, nextCursor != null ? String.valueOf(nextCursor) : null, hasMore);
    }

    @Override
    public CursorResult<PostInfoVO> getRecommendedFeed(String pageToken, int size) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return getFeedList(parseLegacyCursor(pageToken), size);
        }

        try {
            RecommendationFeedResponse response = recommendationService.recommendFeed(currentUserId, pageToken, size, "feed");
            if (response == null || response.getPostIds() == null || response.getPostIds().isEmpty()) {
                return CursorResult.build(new ArrayList<>(), null, false);
            }

            List<Long> postIds = response.getPostIds();
            List<Post> posts = listByIds(postIds);
            Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, post -> post));

            List<Post> sortedPosts = new ArrayList<>();
            for (Long postId : postIds) {
                Post post = postMap.get(postId);
                if (post != null) {
                    sortedPosts.add(post);
                }
            }

            List<PostInfoVO> voList = convertToPostInfoVO(sortedPosts);
            attachRecommendationMeta(voList, response);

            boolean hasMore = Boolean.TRUE.equals(response.getHasMore())
                    || (response.getNextToken() != null && !response.getNextToken().isBlank());
            return CursorResult.build(voList, response.getNextToken(), hasMore);
        } catch (Exception e) {
            return getFeedList(parseLegacyCursor(pageToken), size);
        }
    }

    @Override
    public PostDetailVO getPostDetailById(Long id) {
        Long currentUserId = UserContext.getUserId();
        return baseMapper.selectPostDetail(id, currentUserId);
    }

    @Override
    public boolean likePost(Long postId, boolean isLike) {
        Long userId = UserContext.getUserId();
        if (isLike) {
            // 点赞
            // 1. 检查是否已经点赞
            LambdaQueryWrapper<PostLike> query = new LambdaQueryWrapper<>();
            query.eq(PostLike::getPostId, postId)
                    .eq(PostLike::getUserId, userId);
            if (postLikeMapper.selectCount(query) > 0) {
                return true;
            }

            // 2. 插入点赞记录
            PostLike postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setUserId(userId);
            postLike.setCreatedAt(LocalDateTime.now());
            postLikeMapper.insert(postLike);

            // 3. 更新帖子点赞数
            // update posts set like_count = like_count + 1 where id = postId
            baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("like_count = like_count + 1"));

            // 4. 更新作者的获赞数
            Post post = getById(postId);
            if (post != null) {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, post.getUserId())
                        .setSql("received_like_count = received_like_count + 1"));
            }
        } else {
            // 取消点赞
            // 1. 删除点赞记录
            LambdaQueryWrapper<PostLike> query = new LambdaQueryWrapper<>();
            query.eq(PostLike::getPostId, postId)
                    .eq(PostLike::getUserId, userId);
            int deleted = postLikeMapper.delete(query);

            if (deleted > 0) {
                // 2. 更新帖子点赞数
                baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("like_count = like_count - 1"));

                // 3. 更新作者的获赞数
                Post post = getById(postId);
                if (post != null) {
                    userMapper.update(null, new LambdaUpdateWrapper<User>()
                            .eq(User::getId, post.getUserId())
                            .setSql("received_like_count = received_like_count - 1"));
                }
            }
        }
        return true;
    }

    @Override
    public boolean collectPost(Long postId, boolean isCollect) {
        Long userId = UserContext.getUserId();
        if (isCollect) {
            // 收藏
            LambdaQueryWrapper<PostCollect> query = new LambdaQueryWrapper<>();
            query.eq(PostCollect::getPostId, postId)
                    .eq(PostCollect::getUserId, userId);
            if (postCollectMapper.selectCount(query) > 0) {
                return true;
            }

            PostCollect postCollect = new PostCollect();
            postCollect.setPostId(postId);
            postCollect.setUserId(userId);
            postCollect.setCreatedAt(LocalDateTime.now());
            postCollectMapper.insert(postCollect);

            // 更新帖子收藏数
            baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .setSql("collect_count = collect_count + 1"));

            // 更新作者的被收藏数
            Post post = getById(postId);
            if (post != null) {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, post.getUserId())
                        .setSql("received_collect_count = received_collect_count + 1"));
            }
        } else {
            // 取消收藏
            LambdaQueryWrapper<PostCollect> query = new LambdaQueryWrapper<>();
            query.eq(PostCollect::getPostId, postId)
                    .eq(PostCollect::getUserId, userId);
            int deleted = postCollectMapper.delete(query);

            if (deleted > 0) {
                baseMapper.update(null, new LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("collect_count = collect_count - 1"));

                // 更新作者的被收藏数
                Post post = getById(postId);
                if (post != null) {
                    userMapper.update(null, new LambdaUpdateWrapper<User>()
                            .eq(User::getId, post.getUserId())
                            .setSql("received_collect_count = received_collect_count - 1"));
                }
            }
        }
        return true;
    }

    @Override
    public CursorResult<PostInfoVO> getMyPosts(Long userId, Long lastId, int size) {
        // 1. 构造查询条件
        LambdaQueryWrapper<Post> query = new LambdaQueryWrapper<>();
        query.eq(Post::getUserId, userId);

        // 2. 处理游标
        if (lastId != null) {
            query.lt(Post::getId, lastId);
        }

        // 3. 排序与限制
        query.orderByDesc(Post::getId);
        query.last("LIMIT " + (size + 1));

        // 4. 执行查询
        List<Post> posts = list(query);

        // 5. 处理游标和 hasMore
        boolean hasMore = false;
        Long nextCursor = null;

        if (posts.size() > size) {
            hasMore = true;
            posts.removeLast();
            nextCursor = posts.get(size).getId();
        } else if (!posts.isEmpty()) {
            nextCursor = posts.getLast().getId();
        }

        List<PostInfoVO> voList = convertToPostInfoVO(posts);

        return CursorResult.build(voList, nextCursor != null ? String.valueOf(nextCursor) : null, hasMore);
    }

    @Override
    public CursorResult<PostInfoVO> getMyLikedPosts(Long userId, Long lastId, int size) {
        // 1. 构造查询条件
        LambdaQueryWrapper<PostLike> query = new LambdaQueryWrapper<>();
        query.eq(PostLike::getUserId, userId);

        // 2. 处理游标
        if (lastId != null) {
            query.lt(PostLike::getId, lastId);
        }

        // 3. 排序与限制
        query.orderByDesc(PostLike::getId);
        query.last("LIMIT " + (size + 1));

        // 4. 执行查询
        List<PostLike> likes = postLikeMapper.selectList(query);

        // 5. 处理游标和 hasMore
        boolean hasMore = false;
        Long nextCursor = null;

        if (likes.size() > size) {
            hasMore = true;
            likes.removeLast();
            nextCursor = likes.getLast().getId();
        } else if (!likes.isEmpty()) {
            nextCursor = likes.getLast().getId();
        }

        if (likes.isEmpty()) {
            return CursorResult.build(new ArrayList<>(), null, false);
        }

        // 6. 获取帖子信息并保持顺序
        List<Long> postIds = likes.stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = listByIds(postIds);
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> sortedPosts = new ArrayList<>();
        for (Long postId : postIds) {
            if (postMap.containsKey(postId)) {
                sortedPosts.add(postMap.get(postId));
            }
        }

        List<PostInfoVO> voList = convertToPostInfoVO(sortedPosts);

        return CursorResult.build(voList, nextCursor != null ? String.valueOf(nextCursor) : null, hasMore);
    }

    @Override
    public CursorResult<PostInfoVO> getMyCollectedPosts(Long userId, Long lastId, int size) {
        // 1. 构造查询条件
        LambdaQueryWrapper<PostCollect> query = new LambdaQueryWrapper<>();
        query.eq(PostCollect::getUserId, userId);

        // 2. 处理游标
        if (lastId != null) {
            query.lt(PostCollect::getId, lastId);
        }

        // 3. 排序与限制
        query.orderByDesc(PostCollect::getId);
        query.last("LIMIT " + (size + 1));

        // 4. 执行查询
        List<PostCollect> collects = postCollectMapper.selectList(query);

        // 5. 处理游标和 hasMore
        boolean hasMore = false;
        Long nextCursor = null;

        if (collects.size() > size) {
            hasMore = true;
            collects.removeLast();
            nextCursor = collects.getLast().getId();
        } else if (!collects.isEmpty()) {
            nextCursor = collects.getLast().getId();
        }

        if (collects.isEmpty()) {
            return CursorResult.build(new ArrayList<>(), null, false);
        }

        // 6. 获取帖子信息并保持顺序
        List<Long> postIds = collects.stream()
                .map(PostCollect::getPostId)
                .collect(Collectors.toList());

        List<Post> posts = listByIds(postIds);
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));

        List<Post> sortedPosts = new ArrayList<>();
        for (Long postId : postIds) {
            if (postMap.containsKey(postId)) {
                sortedPosts.add(postMap.get(postId));
            }
        }

        List<PostInfoVO> voList = convertToPostInfoVO(sortedPosts);

        return CursorResult.build(voList, nextCursor != null ? String.valueOf(nextCursor) : null, hasMore);
    }

    private List<PostInfoVO> convertToPostInfoVO(List<Post> posts) {
        // 4. 转换为 PostInfoVO 并填充用户信息
        List<PostInfoVO> voList = new ArrayList<>();
        if (!posts.isEmpty()) {
            // 收集所有 userId
            Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
            // 批量查询用户
            List<User> users = userService.listByIds(userIds);
            // 转为 Map 方便查找
            Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

            // 批量查询当前用户是否点赞
            Long currentUserId = UserContext.getUserId();
            Set<Long> likedPostIds = new HashSet<>();
            Set<Long> collectedPostIds = new HashSet<>();
            if (currentUserId != null) {
                List<Long> currentBatchPostIds = posts.stream().map(Post::getId).collect(Collectors.toList());
                if (!currentBatchPostIds.isEmpty()) {
                    LambdaQueryWrapper<PostLike> likeQuery = new LambdaQueryWrapper<>();
                    likeQuery.eq(PostLike::getUserId, currentUserId)
                            .in(PostLike::getPostId, currentBatchPostIds);
                    List<PostLike> likes = postLikeMapper.selectList(likeQuery);
                    likedPostIds = likes.stream().map(PostLike::getPostId)
                            .collect(Collectors.toSet());

                    LambdaQueryWrapper<PostCollect> collectQuery = new LambdaQueryWrapper<>();
                    collectQuery.eq(PostCollect::getUserId, currentUserId)
                            .in(PostCollect::getPostId, currentBatchPostIds);
                    List<PostCollect> collects = postCollectMapper.selectList(collectQuery);
                    collectedPostIds = collects.stream().map(PostCollect::getPostId)
                            .collect(Collectors.toSet());
                }
            }

            for (Post post : posts) {
                PostInfoVO vo = new PostInfoVO();
                vo.setId(post.getId());
                vo.setTitle(post.getTitle());
                vo.setLikeCount(post.getLikeCount());
                vo.setIsLiked(likedPostIds.contains(post.getId()));
                vo.setCollectCount(post.getCollectCount());
                vo.setIsCollected(collectedPostIds.contains(post.getId()));

                // 设置第一张图片
                List<String> images = post.getImages();
                if (images != null && !images.isEmpty()) {
                    vo.setImage(images.getFirst());
                }

                // 设置宽高
                vo.setWidth(post.getImgWidth());
                vo.setHeight(post.getImgHeight());

                // 设置用户信息
                User user = userMap.get(post.getUserId());
                if (user != null) {
                    vo.setNickname(user.getNickname());
                    vo.setAvatarUrl(user.getAvatarUrl());
                }

                voList.add(vo);
            }
        }
        return voList;
    }

    private void attachRecommendationMeta(List<PostInfoVO> voList, RecommendationFeedResponse response) {
        Map<Long, Integer> postIndexMap = new HashMap<>();
        for (int i = 0; i < response.getPostIds().size(); i++) {
            postIndexMap.put(response.getPostIds().get(i), i);
        }

        int offset = response.getOffset() == null ? 0 : response.getOffset();
        for (PostInfoVO vo : voList) {
            Integer index = postIndexMap.get(vo.getId());
            if (index == null) {
                continue;
            }

            vo.setRecommendRequestId(response.getRequestId());
            vo.setRecallSource(getListValue(response.getRecallSources(), index));
            vo.setRecommendScore(getListValue(response.getScores(), index));
            vo.setFeedPosition(offset + index);
        }
    }

    private Long parseLegacyCursor(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(pageToken);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private <T> T getListValue(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }
}
