package com.rednote.controller;

import com.rednote.common.Result;
import com.rednote.entity.dto.SendMessageDTO;
import com.rednote.entity.vo.ConversationVO;
import com.rednote.entity.vo.MessageVO;
import com.rednote.service.MessageService;
import com.rednote.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 创建单聊会话
     */
    @PostMapping("/conversations/private")
    public Result<String> createPrivateConversation(
            @RequestParam Long targetUserId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token.replace("Bearer ", ""));
            String conversationId = messageService.createPrivateConversation(userId, targetUserId);
            return Result.success(conversationId);
        } catch (Exception e) {
            log.error("Failed to create conversation", e);
            return Result.error("创建会话失败");
        }
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> getConversations(@RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token.replace("Bearer ", ""));
            List<ConversationVO> conversations = messageService.getConversationList(userId);
            return Result.success(conversations);
        } catch (Exception e) {
            log.error("Failed to get conversations", e);
            return Result.error("获取会话列表失败");
        }
    }

    /**
     * 获取消息历史
     */
    @GetMapping("/history/{conversationId}")
    public Result<List<MessageVO>> getMessageHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        try {
            List<MessageVO> messages = messageService.getMessageHistory(conversationId, beforeMessageId, limit);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("Failed to get message history", e);
            return Result.error("获取消息历史失败");
        }
    }

    /**
     * 标记会话为已读
     */
    @PostMapping("/read/{conversationId}")
    public Result<Void> markAsRead(
            @PathVariable String conversationId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token.replace("Bearer ", ""));
            messageService.markAsRead(userId, conversationId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("Failed to mark as read", e);
            return Result.error("标记已读失败");
        }
    }
}
