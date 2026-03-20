package com.rednote.controller;

import com.rednote.common.CursorResult;
import com.rednote.common.Result;
import com.rednote.common.UserContext;
import com.rednote.entity.dto.PostPublishDTO;
import com.rednote.entity.vo.PostDetailVO;
import com.rednote.entity.vo.PostInfoVO;
import com.rednote.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Resource
    private PostService postService;

    // 发布帖子
    @PostMapping("/publish")
    public Result<PostDetailVO> publish(
            PostPublishDTO postPublishDTO,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        return Result.success(postService.publishPost(postPublishDTO, files));
    }

    // 获取帖子详情
    @GetMapping("/{id}")
    public Result<PostDetailVO> getDetail(@PathVariable Long id) {
        return Result.success(postService.getPostDetailById(id));
    }

    // 获取首页流 (Feed)
    @GetMapping("/feed")
    public Result<CursorResult<PostInfoVO>> getFeed(
            @RequestParam(required = false) Long lastId, // 第一次传 null
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.getFeedList(lastId, size));
    }

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result<Boolean> like(@RequestParam Long targetId, @RequestParam boolean isLike) {
        return Result.success(postService.likePost(targetId, isLike));
    }

    // 收藏/取消收藏
    @PostMapping("/collect")
    public Result<Boolean> collect(@RequestParam Long targetId, @RequestParam boolean isCollect) {
        return Result.success(postService.collectPost(targetId, isCollect));
    }

    // 获取“我”发布的帖子
    @GetMapping("/my")
    public Result<CursorResult<PostInfoVO>> getMyPosts(
            @RequestParam(required = false) Long lastId, // 第一次传 null
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(postService.getMyPosts(userId, lastId, size));
    }

    // 获取“我”点赞的帖子
    @GetMapping("/my/liked")
    public Result<CursorResult<PostInfoVO>> getMyLikedPosts(
            @RequestParam(required = false) Long lastId, // 第一次传 null
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(postService.getMyLikedPosts(userId, lastId, size));
    }

    // 获取“我”收藏的帖子
    @GetMapping("/my/collected")
    public Result<CursorResult<PostInfoVO>> getMyCollectedPosts(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(postService.getMyCollectedPosts(userId, lastId, size));
    }
}