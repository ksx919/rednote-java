package com.rednote.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private String conversationId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private String type; // TEXT, IMAGE, VIDEO, AUDIO
    private String content;
    private String extraData; // JSON字符串
    private Long replyToMessageId;
    private Boolean isRecalled;
    private LocalDateTime createdAt;
}
