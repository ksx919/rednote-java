package com.rednote.entity.dto;

import lombok.Data;

@Data
public class PostPublishDTO {

    private String title;

    private String content;

    private String tagsJson;

    private Integer imgWidth;

    private Integer imgHeight;
}
