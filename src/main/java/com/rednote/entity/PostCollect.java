package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "post_collects")
public class PostCollect {
    private Long id;

    private Long postId;

    private Long userId;

    private LocalDateTime createdAt;
}
