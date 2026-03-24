package com.rednote.service;

import com.rednote.recommend.dto.RecommendationFeedResponse;

public interface RecommendationService {

    RecommendationFeedResponse recommendFeed(Long userId, String pageToken, int size, String scene);
}
