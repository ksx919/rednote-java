package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Long senderId;

    private String type; // TEXT, IMAGE, VIDEO, AUDIO, SYSTEM

    private String content;

    private String extraData; // JSON字符串

    private Long replyToMessageId;

    private Boolean isRecalled;

    private LocalDateTime recalledAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
