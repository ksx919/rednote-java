package com.rednote.service;

import com.rednote.entity.dto.FeedExposureBatchDTO;
import com.rednote.entity.dto.PostViewEventDTO;

public interface FeedEventService {

    void recordExposures(Long userId, FeedExposureBatchDTO batchDTO);

    void recordPostView(Long userId, PostViewEventDTO viewEventDTO);
}
