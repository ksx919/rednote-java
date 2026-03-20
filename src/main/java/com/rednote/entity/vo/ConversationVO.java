package com.rednote.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private String id;
    private String type; // PRIVATE, GROUP
    private String name;
    private String avatar;
    private Integer memberCount;

    // 对方用户信息（私聊时使用）
    private Long otherUserId;
    private String otherUserNickname;
    private String otherUserAvatar;

    // 最后消息信息
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Long lastSenderId;
    private String lastSenderNickname;

    // 用户个性化信息
    private Integer unreadCount;
    private Boolean isMuted;
    private Boolean isPinned;

    private LocalDateTime createdAt;
}
