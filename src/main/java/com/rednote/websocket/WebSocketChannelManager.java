package com.rednote.websocket;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket连接管理器
 * 管理用户ID与Channel的映射关系
 */
@Slf4j
@Component
public class WebSocketChannelManager {

    /**
     * 存储所有活跃的Channel
     */
    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户ID -> Channel映射
     */
    private static final Map<Long, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    /**
     * Channel -> 用户ID映射
     */
    private static final Map<String, Long> CHANNEL_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 添加Channel到管理器
     */
    public void addChannel(Channel channel) {
        CHANNEL_GROUP.add(channel);
        log.info("Channel added: {}, total channels: {}", channel.id().asShortText(), CHANNEL_GROUP.size());
    }

    /**
     * 绑定用户ID与Channel
     */
    public void bindUser(Long userId, Channel channel) {
        USER_CHANNEL_MAP.put(userId, channel);
        CHANNEL_USER_MAP.put(channel.id().asShortText(), userId);
        log.info("User {} bound to channel {}", userId, channel.id().asShortText());
    }

    /**
     * 移除Channel
     */
    public void removeChannel(Channel channel) {
        String channelId = channel.id().asShortText();
        Long userId = CHANNEL_USER_MAP.remove(channelId);
        if (userId != null) {
            USER_CHANNEL_MAP.remove(userId);
            log.info("User {} unbound from channel {}", userId, channelId);
        }
        CHANNEL_GROUP.remove(channel);
        log.info("Channel removed: {}, total channels: {}", channelId, CHANNEL_GROUP.size());
    }

    /**
     * 根据用户ID获取Channel
     */
    public Channel getChannel(Long userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    /**
     * 根据Channel获取用户ID
     */
    public Long getUserId(Channel channel) {
        return CHANNEL_USER_MAP.get(channel.id().asShortText());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        Channel channel = USER_CHANNEL_MAP.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return USER_CHANNEL_MAP.size();
    }
}
