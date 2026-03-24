package com.rednote.entity.dto;

import lombok.Data;

@Data
public class PostViewEventDTO {

    private String requestId;

    private Long postId;

    private Integer dwellMs;
}
