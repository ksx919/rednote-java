package com.rednote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation_members")
public class ConversationMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;

    private Long userId;

    private String role; // OWNER, ADMIN, MEMBER

    // 用户个性化设置
    private Integer unreadCount;

    private Long lastReadMessageId;

    private Boolean isMuted;

    private Boolean isPinned;

    private Boolean isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;
}
