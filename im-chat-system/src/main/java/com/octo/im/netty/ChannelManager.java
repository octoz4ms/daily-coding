package com.octo.im.netty;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Channel连接管理器
 * 
 * 管理用户与Channel的映射关系
 * 
 * 面试要点 - 如何管理大量连接？
 * 
 * 1. 单机模式
 *    - ConcurrentHashMap存储用户ID -> Channel映射
 *    - 本项目采用此方案
 * 
 * 2. 分布式模式
 *    - 用户ID -> 服务器节点映射存储在Redis
 *    - 跨节点推送通过RPC或MQ
 *    - 需要实现路由层
 */
@Slf4j
@Component
public class ChannelManager {

    /**
     * 用户ID -> Channel映射
     */
    private final Map<Long, Channel> userChannelMap = new ConcurrentHashMap<>();

    /**
     * Channel ID -> 用户ID映射（反向索引）
     */
    private final Map<String, Long> channelUserMap = new ConcurrentHashMap<>();

    /**
     * 绑定用户和Channel
     */
    public void bindUser(Long userId, Channel channel) {
        // 先移除旧连接
        Channel oldChannel = userChannelMap.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.info("用户重复登录，关闭旧连接: userId={}", userId);
            oldChannel.close();
            channelUserMap.remove(oldChannel.id().asShortText());
        }

        userChannelMap.put(userId, channel);
        channelUserMap.put(channel.id().asShortText(), userId);
        
        log.info("用户上线: userId={}, channelId={}", userId, channel.id().asShortText());
    }

    /**
     * 解绑用户
     */
    public void unbindUser(Channel channel) {
        String channelId = channel.id().asShortText();
        Long userId = channelUserMap.remove(channelId);
        
        if (userId != null) {
            userChannelMap.remove(userId);
            log.info("用户下线: userId={}, channelId={}", userId, channelId);
        }
    }

    /**
     * 获取用户的Channel
     */
    public Channel getChannel(Long userId) {
        return userChannelMap.get(userId);
    }

    /**
     * 获取Channel对应的用户ID
     */
    public Long getUserId(Channel channel) {
        return channelUserMap.get(channel.id().asShortText());
    }

    /**
     * 判断用户是否在线
     */
    public boolean isOnline(Long userId) {
        Channel channel = userChannelMap.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 获取所有在线用户ID
     */
    public Set<Long> getOnlineUserIds() {
        return userChannelMap.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return (int) userChannelMap.values().stream()
                .filter(Channel::isActive)
                .count();
    }

    /**
     * 向用户发送消息
     */
    public boolean sendToUser(Long userId, Object message) {
        Channel channel = userChannelMap.get(userId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message);
            return true;
        }
        return false;
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Object message) {
        userChannelMap.values().stream()
                .filter(Channel::isActive)
                .forEach(channel -> channel.writeAndFlush(message));
    }

    /**
     * 向指定用户列表发送消息
     */
    public void sendToUsers(Set<Long> userIds, Object message) {
        userIds.forEach(userId -> sendToUser(userId, message));
    }
}

