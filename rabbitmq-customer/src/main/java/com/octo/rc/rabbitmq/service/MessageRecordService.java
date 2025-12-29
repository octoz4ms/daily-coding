package com.octo.rc.rabbitmq.service;

import com.octo.rc.rabbitmq.entity.MessageRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地消息表服务
 * 
 * 注意：这里使用内存存储演示，生产环境应该使用数据库
 * 建议使用 MySQL + 定时任务扫描的方式
 */
@Service
@Slf4j
public class MessageRecordService {

    /**
     * 本地消息存储（生产环境使用数据库）
     */
    private final Map<String, MessageRecord> messageStorage = new ConcurrentHashMap<>();

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRY_COUNT = 5;

    /**
     * 创建消息记录
     */
    public MessageRecord createMessageRecord(String businessId, String businessType,
                                              String exchange, String routingKey,
                                              String messageBody) {
        MessageRecord record = new MessageRecord();
        record.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        record.setBusinessId(businessId);
        record.setBusinessType(businessType);
        record.setExchange(exchange);
        record.setRoutingKey(routingKey);
        record.setMessageBody(messageBody);
        record.setStatus(MessageRecord.STATUS_PENDING);
        record.setRetryCount(0);
        record.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        
        messageStorage.put(record.getMessageId(), record);
        log.info("创建消息记录，messageId: {}, businessId: {}", record.getMessageId(), businessId);
        
        return record;
    }

    /**
     * 根据消息ID获取消息记录
     */
    public MessageRecord getByMessageId(String messageId) {
        return messageStorage.get(messageId);
    }

    /**
     * 更新消息状态为发送中
     */
    public void markAsSending(String messageId) {
        MessageRecord record = messageStorage.get(messageId);
        if (record != null) {
            record.setStatus(MessageRecord.STATUS_SENDING);
            record.setUpdateTime(LocalDateTime.now());
            log.debug("消息状态更新为发送中，messageId: {}", messageId);
        }
    }

    /**
     * 更新消息状态为发送成功
     */
    public void markAsSuccess(String messageId) {
        MessageRecord record = messageStorage.get(messageId);
        if (record != null) {
            record.setStatus(MessageRecord.STATUS_SUCCESS);
            record.setUpdateTime(LocalDateTime.now());
            log.info("消息发送成功确认，messageId: {}", messageId);
        }
    }

    /**
     * 更新消息状态为发送失败
     */
    public void markAsFailed(String messageId, String failReason) {
        MessageRecord record = messageStorage.get(messageId);
        if (record != null) {
            record.setStatus(MessageRecord.STATUS_FAILED);
            record.setFailReason(failReason);
            record.setUpdateTime(LocalDateTime.now());
            log.error("消息发送失败，messageId: {}, reason: {}", messageId, failReason);
        }
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount(String messageId) {
        MessageRecord record = messageStorage.get(messageId);
        if (record != null) {
            record.setRetryCount(record.getRetryCount() + 1);
            // 使用指数退避策略计算下次重试时间
            int delaySeconds = (int) Math.pow(2, record.getRetryCount()) * 10;
            record.setNextRetryTime(LocalDateTime.now().plusSeconds(delaySeconds));
            record.setStatus(MessageRecord.STATUS_PENDING);
            record.setUpdateTime(LocalDateTime.now());
            log.info("消息重试次数增加，messageId: {}, retryCount: {}, nextRetryTime: {}", 
                    messageId, record.getRetryCount(), record.getNextRetryTime());
        }
    }

    /**
     * 获取需要重发的消息列表
     * 
     * 条件：
     * 1. 状态为待发送或发送中
     * 2. 重试次数未超过最大值
     * 3. 下次重试时间已到或为空
     */
    public List<MessageRecord> getMessagesToResend() {
        LocalDateTime now = LocalDateTime.now();
        return messageStorage.values().stream()
                .filter(record -> 
                    (record.getStatus() == MessageRecord.STATUS_PENDING 
                        || record.getStatus() == MessageRecord.STATUS_SENDING)
                    && record.getRetryCount() < record.getMaxRetryCount()
                    && (record.getNextRetryTime() == null || record.getNextRetryTime().isBefore(now))
                )
                .collect(Collectors.toList());
    }

    /**
     * 获取所有消息记录（用于监控）
     */
    public List<MessageRecord> getAllRecords() {
        return new ArrayList<>(messageStorage.values());
    }

    /**
     * 删除已成功的消息记录（可选，用于清理）
     */
    public void cleanSuccessMessages(int keepDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(keepDays);
        messageStorage.values().removeIf(record -> 
            record.getStatus() == MessageRecord.STATUS_SUCCESS 
            && record.getUpdateTime().isBefore(threshold)
        );
        log.info("清理 {} 天前的成功消息记录", keepDays);
    }
}

