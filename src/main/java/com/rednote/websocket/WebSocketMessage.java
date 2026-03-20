package com.rednote.websocket;

import lombok.Data;

/**
 * WebSocket消息协议
 */
@Data
public class WebSocketMessage {
    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID（单聊时使用）
     */
    private Long receiverId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息ID（服务器生成后返回）
     */
    private Long messageId;

    /**
     * 消息内容类型
     */
    private String contentType; // TEXT, IMAGE, VIDEO, AUDIO

    /**
     * 额外数据（JSON字符串）
     */
    private String extraData;

    /**
     * 回复的消息ID
     */
    private Long replyToMessageId;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 错误信息（发生错误时使用）
     */
    private String error;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 客户端发送
        CHAT,           // 发送聊天消息
        READ,           // 标记已读
        TYPING,         // 正在输入
        HEARTBEAT,      // 心跳

        // 服务器响应
        ACK,            // 消息确认
        MESSAGE,        // 推送新消息
        ERROR,          // 错误响应
        ONLINE_STATUS   // 在线状态变更
    }
}
