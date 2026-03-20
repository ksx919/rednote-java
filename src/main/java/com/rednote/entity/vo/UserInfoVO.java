package com.rednote.entity.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer followingCount;
    private Integer followerCount;
    private Integer receivedLikeCount;
    private Integer receivedCollectCount;
    private Integer totalLikedCollected;
}
