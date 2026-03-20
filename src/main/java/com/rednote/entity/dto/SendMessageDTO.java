package com.rednote.entity.dto;

import lombok.Data;

@Data
public class SendMessageDTO {
    private String conversationId;
    private String content;
    private String type; // TEXT, IMAGE, VIDEO, AUDIO
    private String extraData; // JSON字符串，存储图片URL等
    private Long replyToMessageId; // 回复的消息ID
}
