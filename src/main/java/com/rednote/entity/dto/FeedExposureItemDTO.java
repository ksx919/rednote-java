package com.rednote.entity.dto;

import lombok.Data;

@Data
public class FeedExposureItemDTO {

    private String requestId;

    private Long postId;

    private Integer position;

    private String recallSource;

    private Double rankScore;
}
