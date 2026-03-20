package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversations")
public class Conversation {

    @TableId(type = IdType.INPUT) // 会话ID由程序生成，不是自增
    private String id;

    private String type; // PRIVATE, GROUP

    private String name;

    private String avatar;

    private Long creatorId;

    private Integer memberCount;

    // 最后消息信息（冗余字段）
    private Long lastMessageId;

    private String lastMessageContent;

    private LocalDateTime lastMessageTime;

    private Long lastSenderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
