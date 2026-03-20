package com.rednote.controller;

import com.rednote.common.Result;
import com.rednote.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
public class RelationController {

    @Resource
    private RelationService relationService;

    // 关注/取关
    @PostMapping("/follow")
    public Result<Boolean> follow(@RequestParam Long targetUserId, @RequestParam boolean isFollow) {
        relationService.followUser(targetUserId, isFollow);
        return Result.success(true);
    }
}
