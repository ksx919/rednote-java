package com.rednote.service.impl;

import com.rednote.entity.FeedExposureEvent;
import com.rednote.entity.PostViewEvent;
import com.rednote.entity.dto.FeedExposureBatchDTO;
import com.rednote.entity.dto.FeedExposureItemDTO;
import com.rednote.entity.dto.PostViewEventDTO;
import com.rednote.mapper.FeedExposureEventMapper;
import com.rednote.mapper.PostViewEventMapper;
import com.rednote.service.FeedEventService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeedEventServiceImpl implements FeedEventService {

    @Resource
    private FeedExposureEventMapper feedExposureEventMapper;

    @Resource
    private PostViewEventMapper postViewEventMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordExposures(Long userId, FeedExposureBatchDTO batchDTO) {
        if (userId == null || batchDTO == null || batchDTO.getItems() == null || batchDTO.getItems().isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (FeedExposureItemDTO item : batchDTO.getItems()) {
            if (item == null || item.getPostId() == null || item.getRequestId() == null || item.getRequestId().isBlank()) {
                continue;
            }

            FeedExposureEvent event = new FeedExposureEvent();
            event.setRequestId(item.getRequestId());
            event.setUserId(userId);
            event.setPostId(item.getPostId());
            event.setPosition(item.getPosition());
            event.setRecallSource(item.getRecallSource());
            event.setRankScore(item.getRankScore());
            event.setShownAt(now);
            feedExposureEventMapper.insert(event);
        }
    }

    @Override
    public void recordPostView(Long userId, PostViewEventDTO viewEventDTO) {
        if (userId == null || viewEventDTO == null || viewEventDTO.getPostId() == null) {
            return;
        }

        PostViewEvent event = new PostViewEvent();
        event.setRequestId(viewEventDTO.getRequestId());
        event.setUserId(userId);
        event.setPostId(viewEventDTO.getPostId());
        event.setDwellMs(viewEventDTO.getDwellMs());
        event.setViewedAt(LocalDateTime.now());
        postViewEventMapper.insert(event);
    }
}
