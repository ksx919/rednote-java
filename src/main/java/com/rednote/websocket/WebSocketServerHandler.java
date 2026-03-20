package com.rednote.websocket;

import cn.hutool.json.JSONUtil;
import com.rednote.service.MessageService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息处理器
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Autowired
    private WebSocketChannelManager channelManager;

    @Autowired
    private MessageService messageService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelManager.addChannel(ctx.channel());
        log.info("Client connected: {}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelManager.removeChannel(ctx.channel());
        log.info("Client disconnected: {}", ctx.channel().id().asShortText());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, text);
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, String text) {
        try {
            WebSocketMessage message = JSONUtil.toBean(text, WebSocketMessage.class);

            switch (message.getType()) {
                case CHAT:
                    handleChatMessage(ctx, message);
                    break;
                case READ:
                    handleReadMessage(ctx, message);
                    break;
                case TYPING:
                    handleTypingMessage(ctx, message);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(ctx, message);
                    break;
                default:
                    sendError(ctx, "Unknown message type");
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", text, e);
            sendError(ctx, "Invalid message format");
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            // 绑定用户（如果还未绑定）
            if (channelManager.getUserId(ctx.channel()) == null) {
                channelManager.bindUser(message.getSenderId(), ctx.channel());
            }

            // 保存消息到数据库
            Long messageId = messageService.saveMessage(message);
            message.setMessageId(messageId);

            // 发送ACK给发送者
            WebSocketMessage ack = new WebSocketMessage();
            ack.setType(WebSocketMessage.MessageType.ACK);
            ack.setMessageId(messageId);
            ack.setTimestamp(System.currentTimeMillis());
            sendMessage(ctx.channel(), ack);

            // 推送消息给接收者
            messageService.pushMessageToReceivers(message);

        } catch (Exception e) {
            log.error("Error handling chat message", e);
            sendError(ctx, "Failed to send message: " + e.getMessage());
        }
    }

    /**
     * 处理已读消息
     */
    private void handleReadMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            Long userId = channelManager.getUserId(ctx.channel());
            if (userId == null) {
                sendError(ctx, "User not authenticated");
                return;
            }

            messageService.markAsRead(userId, message.getConversationId());

            // 发送ACK
            WebSocketMessage ack = new WebSocketMessage();
            ack.setType(WebSocketMessage.MessageType.ACK);
            ack.setTimestamp(System.currentTimeMillis());
            sendMessage(ctx.channel(), ack);

        } catch (Exception e) {
            log.error("Error handling read message", e);
            sendError(ctx, "Failed to mark as read");
        }
    }

    /**
     * 处理正在输入消息
     */
    private void handleTypingMessage(ChannelHandlerContext ctx, WebSocketMessage message) {
        try {
            // 转发给接收者
            if (message.getReceiverId() != null) {
                io.netty.channel.Channel receiverChannel = channelManager.getChannel(message.getReceiverId());
                if (receiverChannel != null && receiverChannel.isActive()) {
                    sendMessage(receiverChannel, message);
                }
            }
        } catch (Exception e) {
            log.error("Error handling typing message", e);
        }
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, WebSocketMessage message) {
        WebSocketMessage pong = new WebSocketMessage();
        pong.setType(WebSocketMessage.MessageType.HEARTBEAT);
        pong.setTimestamp(System.currentTimeMillis());
        sendMessage(ctx.channel(), pong);
    }

    /**
     * 发送消息到指定Channel
     */
    private void sendMessage(io.netty.channel.Channel channel, WebSocketMessage message) {
        if (channel != null && channel.isActive()) {
            String json = JSONUtil.toJsonStr(message);
            channel.writeAndFlush(new TextWebSocketFrame(json));
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(ChannelHandlerContext ctx, String error) {
        WebSocketMessage errorMsg = new WebSocketMessage();
        errorMsg.setType(WebSocketMessage.MessageType.ERROR);
        errorMsg.setError(error);
        errorMsg.setTimestamp(System.currentTimeMillis());
        sendMessage(ctx.channel(), errorMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("WebSocket error", cause);
        ctx.close();
    }
}
