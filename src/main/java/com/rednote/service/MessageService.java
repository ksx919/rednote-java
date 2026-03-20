package com.rednote.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rednote.entity.Conversation;
import com.rednote.entity.ConversationMember;
import com.rednote.entity.Message;
import com.rednote.entity.User;
import com.rednote.entity.dto.SendMessageDTO;
import com.rednote.entity.vo.ConversationVO;
import com.rednote.entity.vo.MessageVO;
import com.rednote.mapper.ConversationMapper;
import com.rednote.mapper.ConversationMemberMapper;
import com.rednote.mapper.MessageMapper;
import com.rednote.mapper.UserMapper;
import com.rednote.websocket.WebSocketChannelManager;
import com.rednote.websocket.WebSocketMessage;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务
 */
@Slf4j
@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketChannelManager channelManager;

    /**
     * 创建单聊会话
     */
    @Transactional(rollbackFor = Exception.class)
    public String createPrivateConversation(Long userId1, Long userId2) {
        // 生成会话ID（小ID在前）
        Long smallerId = Math.min(userId1, userId2);
        Long largerId = Math.max(userId1, userId2);
        String conversationId = smallerId + "_" + largerId;

        // 检查会话是否已存在
        Conversation existing = conversationMapper.selectById(conversationId);
        if (existing != null) {
            return conversationId;
        }

        // 创建会话
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setType("PRIVATE");
        conversation.setMemberCount(2);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conversation);

        // 添加会话成员
        ConversationMember member1 = new ConversationMember();
        member1.setConversationId(conversationId);
        member1.setUserId(userId1);
        member1.setRole("MEMBER");
        member1.setUnreadCount(0);
        member1.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(member1);

        ConversationMember member2 = new ConversationMember();
        member2.setConversationId(conversationId);
        member2.setUserId(userId2);
        member2.setRole("MEMBER");
        member2.setUnreadCount(0);
        member2.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(member2);

        log.info("Created private conversation: {}", conversationId);
        return conversationId;
    }

    /**
     * 保存消息到数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveMessage(WebSocketMessage wsMessage) {
        // 检查会话是否存在，如果是单聊且不存在则自动创建
        ensureConversationExists(wsMessage);

        // 创建消息实体
        Message message = new Message();
        message.setConversationId(wsMessage.getConversationId());
        message.setSenderId(wsMessage.getSenderId());
        message.setContent(wsMessage.getContent());
        message.setType(wsMessage.getContentType());
        message.setExtraData(wsMessage.getExtraData());
        message.setReplyToMessageId(wsMessage.getReplyToMessageId());
        message.setCreatedAt(LocalDateTime.now());

        // 保存消息
        messageMapper.insert(message);

        // 更新会话信息
        updateConversation(message);

        // 更新未读数
        updateUnreadCount(message);

        return message.getId();
    }

    /**
     * 确保会话存在，如果是单聊且不存在则自动创建
     */
    private void ensureConversationExists(WebSocketMessage wsMessage) {
        String conversationId = wsMessage.getConversationId();
        Conversation conversation = conversationMapper.selectById(conversationId);

        if (conversation == null) {
            // 判断是否是单聊格式（格式：userId1_userId2）
            if (conversationId.matches("\\d+_\\d+")) {
                String[] parts = conversationId.split("_");
                Long userId1 = Long.parseLong(parts[0]);
                Long userId2 = Long.parseLong(parts[1]);
                createPrivateConversation(userId1, userId2);
                log.info("Auto-created conversation: {}", conversationId);
            } else {
                throw new RuntimeException("Conversation not found: " + conversationId);
            }
        }
    }

    /**
     * 更新会话信息
     */
    private void updateConversation(Message message) {
        Conversation conversation = conversationMapper.selectById(message.getConversationId());
        if (conversation != null) {
            conversation.setLastMessageId(message.getId());
            conversation.setLastMessageContent(message.getContent());
            conversation.setLastMessageTime(message.getCreatedAt());
            conversation.setLastSenderId(message.getSenderId());
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    /**
     * 更新未读数
     */
    private void updateUnreadCount(Message message) {
        // 查询会话的所有成员
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, message.getConversationId())
               .ne(ConversationMember::getUserId, message.getSenderId());

        List<ConversationMember> members = conversationMemberMapper.selectList(wrapper);

        // 增加其他成员的未读数
        for (ConversationMember member : members) {
            member.setUnreadCount(member.getUnreadCount() + 1);
            conversationMemberMapper.updateById(member);
        }
    }

    /**
     * 推送消息给接收者
     */
    public void pushMessageToReceivers(WebSocketMessage message) {
        // 查询会话的所有成员（除了发送者）
        LambdaQueryWrapper<ConversationMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, message.getConversationId())
               .ne(ConversationMember::getUserId, message.getSenderId());

        List<ConversationMember> members = conversationMemberMapper.selectList(wrapper);

        // 构造推送消息
        WebSocketMessage pushMessage = new WebSocketMessage();
        pushMessage.setType(WebSocketMessage.MessageType.MESSAGE);
        pushMessage.setMessageId(message.getMessageId());
        pushMessage.setSenderId(message.getSenderId());
        pushMessage.setConversationId(message.getConversationId());
        pushMessage.setContent(message.getContent());
        pushMessage.setContentType(message.getContentType());
        pushMessage.setExtraData(message.getExtraData());
        pushMessage.setReplyToMessageId(message.getReplyToMessageId());
        pushMessage.setTimestamp(System.currentTimeMillis());

        String json = JSONUtil.toJsonStr(pushMessage);

        // 推送给在线的接收者
        for (ConversationMember member : members) {
            Channel channel = channelManager.getChannel(member.getUserId());
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(json));
                log.info("Message pushed to user: {}", member.getUserId());
            }
        }
    }

    /**
     * 标记消息为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, String conversationId) {
        LambdaUpdateWrapper<ConversationMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ConversationMember::getConversationId, conversationId)
               .eq(ConversationMember::getUserId, userId)
               .set(ConversationMember::getUnreadCount, 0);

        conversationMemberMapper.update(null, wrapper);
        log.info("User {} marked conversation {} as read", userId, conversationId);
    }

    /**
     * 获取用户的会话列表
     */
    public List<ConversationVO> getConversationList(Long userId) {
        // 查询用户参与的所有会话
        LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(ConversationMember::getUserId, userId);
        List<ConversationMember> members = conversationMemberMapper.selectList(memberWrapper);

        List<ConversationVO> result = new ArrayList<>();
        for (ConversationMember member : members) {
            Conversation conversation = conversationMapper.selectById(member.getConversationId());
            if (conversation != null) {
                ConversationVO vo = new ConversationVO();
                BeanUtils.copyProperties(conversation, vo);
                vo.setUnreadCount(member.getUnreadCount());

                // 如果是私聊，填充对方用户信息
                if ("PRIVATE".equals(conversation.getType())) {
                    String conversationId = conversation.getId();
                    String[] userIds = conversationId.split("_");
                    if (userIds.length == 2) {
                        Long otherUserId = Long.parseLong(userIds[0]);
                        if (otherUserId.equals(userId)) {
                            otherUserId = Long.parseLong(userIds[1]);
                        }

                        // 查询对方用户信息
                        User otherUser = userMapper.selectById(otherUserId);
                        if (otherUser != null) {
                            vo.setOtherUserId(otherUserId);
                            vo.setOtherUserNickname(otherUser.getNickname());
                            vo.setOtherUserAvatar(otherUser.getAvatarUrl());
                        }
                    }
                }

                result.add(vo);
            }
        }

        // 按最后消息时间倒序排序
        result.sort((a, b) -> {
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return result;
    }

    /**
     * 获取消息历史
     */
    public List<MessageVO> getMessageHistory(String conversationId, Long beforeMessageId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId);

        if (beforeMessageId != null) {
            wrapper.lt(Message::getId, beforeMessageId);
        }

        wrapper.orderByDesc(Message::getId).last("LIMIT " + limit);

        List<Message> messages = messageMapper.selectList(wrapper);
        // 查询是倒序（取最新N条），翻转为正序（旧消息在前，新消息在后）
        java.util.Collections.reverse(messages);

        return messages.stream().map(msg -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(msg, vo);

            // 填充发送者信息
            if (msg.getSenderId() != null) {
                User sender = userMapper.selectById(msg.getSenderId());
                if (sender != null) {
                    vo.setSenderNickname(sender.getNickname());
                    vo.setSenderAvatar(sender.getAvatarUrl());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
