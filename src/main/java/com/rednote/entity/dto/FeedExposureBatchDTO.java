package com.rednote.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeedExposureBatchDTO {

    private List<FeedExposureItemDTO> items;
}
