package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_view_events")
public class PostViewEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long userId;

    private Long postId;

    private Integer dwellMs;

    private LocalDateTime viewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
