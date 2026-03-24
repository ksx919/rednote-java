package com.rednote.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RecommendationFeedResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("post_ids")
    private List<Long> postIds;

    private List<Double> scores;

    @JsonProperty("recall_sources")
    private List<String> recallSources;

    @JsonProperty("next_token")
    private String nextToken;

    @JsonProperty("has_more")
    private Boolean hasMore;

    private Integer offset;
}
