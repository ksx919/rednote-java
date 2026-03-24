package com.rednote.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecommendationFeedRequest {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("page_size")
    private Integer pageSize;

    @JsonProperty("page_token")
    private String pageToken;

    private String scene;
}
