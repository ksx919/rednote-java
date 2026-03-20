package com.rednote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rednote.common.CursorResult;
import com.rednote.entity.Post;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.entity.vo.PostDetailVO;
import org.springframework.web.multipart.MultipartFile;

public interface PostService extends IService<Post> {
    // 发布帖子
    PostDetailVO publishPost(PostPublishDTO postPublishDTO, MultipartFile[] files);

    // 用游标分页获取推荐流
    // lastId: 上一页最后一条的ID (可为空)
    // size: 每次加载多少条
    CursorResult<PostInfoVO> getFeedList(Long lastId, int size);

    PostDetailVO getPostDetailById(Long id);

    // 点赞/取消点赞
    boolean likePost(Long postId, boolean isLike);

    // 收藏/取消收藏
    boolean collectPost(Long postId, boolean isCollect);

    // 获取用户发布的帖子
    CursorResult<PostInfoVO> getMyPosts(Long userId, Long lastId, int size);

    // 获取用户点赞的帖子
    CursorResult<PostInfoVO> getMyLikedPosts(Long userId, Long lastId, int size);

    // 获取用户收藏的帖子
    CursorResult<PostInfoVO> getMyCollectedPosts(Long userId, Long lastId, int size);
}