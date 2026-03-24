package com.rednote.service.impl;

import com.rednote.recommend.dto.RecommendationFeedRequest;
import com.rednote.recommend.dto.RecommendationFeedResponse;
import com.rednote.service.RecommendationService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Value("${recommendation.service.base-url:http://127.0.0.1:8001}")
    private String baseUrl;

    @Value("${recommendation.service.connect-timeout-ms:1500}")
    private int connectTimeoutMs;

    @Value("${recommendation.service.read-timeout-ms:3000}")
    private int readTimeoutMs;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);

        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public RecommendationFeedResponse recommendFeed(Long userId, String pageToken, int size, String scene) {
        RecommendationFeedRequest request = new RecommendationFeedRequest();
        request.setUserId(userId);
        request.setPageSize(size);
        request.setPageToken(pageToken);
        request.setScene(scene);

        return restClient.post()
                .uri("/recommend/feed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RecommendationFeedResponse.class);
    }
}
