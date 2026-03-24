package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feed_exposure_events")
public class FeedExposureEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long userId;

    private Long postId;

    private Integer position;

    private String recallSource;

    private Double rankScore;

    private LocalDateTime shownAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
