package com.rednote.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostInfoVO {
    private Long id;

    private String title;

    private String nickname;

    private String avatarUrl;

    private Integer likeCount;

    private String image;

    private Integer height;

    private Integer width;

    private Boolean isLiked;

    private Integer collectCount;

    private Boolean isCollected;

    private Boolean isFollowed;

    private List<String> tags;

    private String recommendRequestId;

    private String recallSource;

    private Double recommendScore;

    private Integer feedPosition;
}
