package com.octo.im.service;

import com.alibaba.fastjson2.JSON;
import com.octo.im.entity.Message;
import com.octo.im.entity.OfflineMessage;
import com.octo.im.mapper.ConversationMemberMapper;
import com.octo.im.mapper.MessageMapper;
import com.octo.im.mapper.OfflineMessageMapper;
import com.octo.im.netty.ChannelManager;
import com.octo.im.protocol.MessageProtocol;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 消息服务
 * 
 * 面试重点：消息可靠投递机制
 * 
 * 1. 消息存储：先持久化到数据库，再推送
 * 2. 在线投递：通过WebSocket直接推送，等待ACK
 * 3. 离线存储：存入离线消息表，上线后拉取
 * 4. 消息重试：未收到ACK的消息定时重发
 * 5. 消息去重：通过唯一msgId保证幂等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final OfflineMessageMapper offlineMessageMapper;
    private final ConversationMemberMapper memberMapper;
    private final ChannelManager channelManager;
    private final StringRedisTemplate redisTemplate;

    @Value("${im.read-status-prefix}")
    private String readStatusPrefix;

    /**
     * 保存并发送私聊消息
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAndSendMessage(MessageProtocol protocol) {
        // 1. 保存消息到数据库
        Message message = buildMessage(protocol);
        messageMapper.insert(message);
        
        log.info("消息已保存: msgId={}, from={}, to={}", 
                protocol.getMsgId(), protocol.getSenderId(), protocol.getReceiverId());

        // 2. 推送给接收方
        Long receiverId = protocol.getReceiverId();
        boolean sent = channelManager.sendToUser(receiverId, 
                new TextWebSocketFrame(JSON.toJSONString(protocol)));

        if (!sent) {
            // 3. 接收方离线，保存到离线消息表
            saveOfflineMessage(receiverId, protocol.getMsgId(), protocol.getConversationId());
            log.info("接收方离线，消息已存入离线表: userId={}, msgId={}", receiverId, protocol.getMsgId());
        }
    }

    /**
     * 保存并广播群聊消息
     * 
     * 群聊消息扩散策略：写扩散
     * - 给每个在线成员推送消息
     * - 离线成员存入离线消息表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAndBroadcastGroupMessage(MessageProtocol protocol) {
        // 1. 保存消息
        Message message = buildMessage(protocol);
        messageMapper.insert(message);

        // 2. 获取群成员（排除发送者）
        List<Long> memberIds = memberMapper.getOtherMemberIds(
                protocol.getConversationId(), protocol.getSenderId());

        // 3. 分发消息
        Set<Long> offlineUsers = new HashSet<>();
        String msgJson = JSON.toJSONString(protocol);
        
        for (Long memberId : memberIds) {
            boolean sent = channelManager.sendToUser(memberId, new TextWebSocketFrame(msgJson));
            if (!sent) {
                offlineUsers.add(memberId);
            }
        }

        // 4. 保存离线消息
        for (Long userId : offlineUsers) {
            saveOfflineMessage(userId, protocol.getMsgId(), protocol.getConversationId());
        }

        log.info("群消息已发送: msgId={}, online={}, offline={}", 
                protocol.getMsgId(), memberIds.size() - offlineUsers.size(), offlineUsers.size());
    }

    /**
     * 推送离线消息
     */
    @Async
    public void pushOfflineMessages(Long userId) {
        List<OfflineMessage> offlineMessages = offlineMessageMapper.getByUserId(userId);
        
        if (offlineMessages.isEmpty()) {
            return;
        }

        log.info("推送离线消息: userId={}, count={}", userId, offlineMessages.size());

        for (OfflineMessage offline : offlineMessages) {
            // 查询消息详情
            Message message = messageMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message>()
                            .eq("msg_id", offline.getMsgId()));
            
            if (message != null) {
                MessageProtocol protocol = buildProtocol(message);
                channelManager.sendToUser(userId, new TextWebSocketFrame(JSON.toJSONString(protocol)));
            }
        }

        // 删除已推送的离线消息
        offlineMessageMapper.deleteByUserId(userId);
    }

    /**
     * 更新消息状态
     */
    public void updateMessageStatus(String msgId, Integer status) {
        messageMapper.updateStatus(msgId, status);
        log.debug("消息状态更新: msgId={}, status={}", msgId, status);
    }

    /**
     * 标记消息已读
     * 
     * 已读状态存储策略：
     * 1. Redis缓存：快速查询，设置过期时间
     * 2. 数据库持久化：定时同步或异步写入
     */
    public void markAsRead(String msgId, Long userId) {
        // 1. 更新Redis已读状态
        String key = readStatusPrefix + msgId;
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, 7, TimeUnit.DAYS);

        // 2. 通知发送方（已读回执）
        Message message = messageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message>()
                        .eq("msg_id", msgId));
        
        if (message != null) {
            MessageProtocol readReceipt = MessageProtocol.builder()
                    .cmd(MessageProtocol.CmdType.MSG_READ.getCode())
                    .msgId(msgId)
                    .senderId(userId)
                    .receiverId(message.getSenderId())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            channelManager.sendToUser(message.getSenderId(), 
                    new TextWebSocketFrame(JSON.toJSONString(readReceipt)));
        }

        log.debug("消息已读: msgId={}, userId={}", msgId, userId);
    }

    /**
     * 同步消息
     */
    public void syncMessages(Long userId, Long sinceTimestamp) {
        LocalDateTime since = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(sinceTimestamp != null ? sinceTimestamp : 0), 
                ZoneId.systemDefault());
        
        List<Message> messages = messageMapper.getMessagesSince(userId, since);
        
        for (Message message : messages) {
            MessageProtocol protocol = buildProtocol(message);
            channelManager.sendToUser(userId, new TextWebSocketFrame(JSON.toJSONString(protocol)));
        }
        
        log.info("消息同步完成: userId={}, count={}", userId, messages.size());
    }

    /**
     * 保存离线消息
     */
    private void saveOfflineMessage(Long userId, String msgId, Long conversationId) {
        OfflineMessage offline = new OfflineMessage();
        offline.setUserId(userId);
        offline.setMsgId(msgId);
        offline.setConversationId(conversationId);
        offlineMessageMapper.insert(offline);
    }

    /**
     * 构建消息实体
     */
    private Message buildMessage(MessageProtocol protocol) {
        Message message = new Message();
        message.setMsgId(protocol.getMsgId());
        message.setConversationId(protocol.getConversationId());
        message.setSenderId(protocol.getSenderId());
        message.setMsgType(protocol.getMsgType());
        message.setContent(protocol.getContent());
        message.setExtra(protocol.getExtra());
        message.setStatus(Message.Status.SENT.getCode());
        message.setSendTime(LocalDateTime.now());
        return message;
    }

    /**
     * 构建消息协议
     */
    private MessageProtocol buildProtocol(Message message) {
        return MessageProtocol.builder()
                .cmd(MessageProtocol.CmdType.CHAT_MSG.getCode())
                .msgId(message.getMsgId())
                .senderId(message.getSenderId())
                .conversationId(message.getConversationId())
                .msgType(message.getMsgType())
                .content(message.getContent())
                .extra(message.getExtra())
                .timestamp(message.getSendTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
    }
}

