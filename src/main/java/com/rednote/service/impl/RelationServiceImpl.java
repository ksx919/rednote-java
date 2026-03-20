package com.rednote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rednote.common.UserContext;
import com.rednote.entity.User;
import com.rednote.entity.UserRelation;
import com.rednote.mapper.UserRelationMapper;
import com.rednote.mapper.UserMapper;
import com.rednote.service.RelationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RelationServiceImpl extends ServiceImpl<UserRelationMapper, UserRelation> implements RelationService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long targetUserId, boolean isFollow) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException("不能关注自己");
        }

        if (isFollow) {
            // 关注
            // 1. 检查是否已关注
            LambdaQueryWrapper<UserRelation> query = new LambdaQueryWrapper<>();
            query.eq(UserRelation::getFollowerId, currentUserId)
                    .eq(UserRelation::getFollowingId, targetUserId);
            if (count(query) > 0) {
                return;
            }

            // 2. 插入记录
            UserRelation relation = new UserRelation();
            relation.setFollowerId(currentUserId);
            relation.setFollowingId(targetUserId);
            relation.setCreatedAt(LocalDateTime.now());
            save(relation);

            // 3. 更新当前用户的关注数 (+1)
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, currentUserId)
                    .setSql("following_count = following_count + 1"));

            // 4. 更新目标用户的粉丝数 (+1)
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, targetUserId)
                    .setSql("follower_count = follower_count + 1"));

        } else {
            // 取关
            LambdaQueryWrapper<UserRelation> query = new LambdaQueryWrapper<>();
            query.eq(UserRelation::getFollowerId, currentUserId)
                    .eq(UserRelation::getFollowingId, targetUserId);
            boolean removed = remove(query);

            if (removed) {
                // 更新当前用户的关注数 (-1)
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, currentUserId)
                        .setSql("following_count = following_count - 1"));

                // 更新目标用户的粉丝数 (-1)
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, targetUserId)
                        .setSql("follower_count = follower_count - 1"));
            }
        }
    }
}
