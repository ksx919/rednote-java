package com.rednote.controller;

import com.rednote.common.Result;
import com.rednote.common.UserContext;
import com.rednote.entity.dto.FeedExposureBatchDTO;
import com.rednote.service.FeedEventService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
public class FeedController {

    @Resource
    private FeedEventService feedEventService;

    @PostMapping("/exposure/batch")
    public Result<Boolean> batchRecordExposure(@RequestBody FeedExposureBatchDTO batchDTO) {
        feedEventService.recordExposures(UserContext.getUserId(), batchDTO);
        return Result.success(true);
    }
}
