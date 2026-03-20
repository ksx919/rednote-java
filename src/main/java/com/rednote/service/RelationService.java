package com.rednote.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rednote.entity.UserRelation;

public interface RelationService extends IService<UserRelation> {
    /**
     * 关注/取关
     * 
     * @param targetUserId 目标用户ID
     * @param isFollow     true=关注, false=取关
     */
    void followUser(Long targetUserId, boolean isFollow);
}
