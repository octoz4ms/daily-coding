package com.octo.rc.rabbitmq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息幂等性服务
 * 
 * 用于防止消息重复消费：
 * 1. 消费前检查消息是否已处理
 * 2. 消费成功后标记消息已处理
 * 
 * 注意：生产环境建议使用 Redis 实现，支持分布式场景和过期时间
 */
@Service
@Slf4j
public class IdempotentService {

    /**
     * 已处理消息缓存
     * Key: messageId
     * Value: 处理时间
     * 
     * 生产环境建议使用 Redis：
     * SET messageId:consumed "1" EX 86400
     */
    private final Map<String, LocalDateTime> consumedMessages = new ConcurrentHashMap<>();

    /**
     * 消息过期时间（小时）
     */
    private static final int EXPIRE_HOURS = 24;

    /**
     * 检查消息是否已被消费
     * 
     * @param messageId 消息ID
     * @return true-已消费，false-未消费
     */
    public boolean isConsumed(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            log.warn("消息ID为空，无法进行幂等性检查");
            return false;
        }

        LocalDateTime consumedTime = consumedMessages.get(messageId);
        if (consumedTime != null) {
            // 检查是否过期
            if (consumedTime.plusHours(EXPIRE_HOURS).isAfter(LocalDateTime.now())) {
                log.warn("消息已被消费，messageId: {}, consumedTime: {}", messageId, consumedTime);
                return true;
            } else {
                // 已过期，移除记录
                consumedMessages.remove(messageId);
            }
        }
        return false;
    }

    /**
     * 标记消息已被消费
     * 
     * @param messageId 消息ID
     */
    public void markAsConsumed(String messageId) {
        if (messageId != null && !messageId.isEmpty()) {
            consumedMessages.put(messageId, LocalDateTime.now());
            log.debug("消息标记为已消费，messageId: {}", messageId);
        }
    }

    /**
     * 尝试获取消费锁（原子操作）
     * 
     * 用于分布式场景下的并发控制
     * 生产环境使用 Redis SETNX 实现
     * 
     * @param messageId 消息ID
     * @return true-获取成功，false-已被其他消费者获取
     */
    public synchronized boolean tryAcquire(String messageId) {
        if (isConsumed(messageId)) {
            return false;
        }
        // 预先标记，防止并发重复消费
        consumedMessages.put(messageId, LocalDateTime.now());
        return true;
    }

    /**
     * 释放消费锁（消费失败时调用）
     * 
     * @param messageId 消息ID
     */
    public void release(String messageId) {
        consumedMessages.remove(messageId);
        log.debug("消费锁已释放，messageId: {}", messageId);
    }

    /**
     * 清理过期的消费记录
     */
    public void cleanExpiredRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(EXPIRE_HOURS);
        consumedMessages.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
        log.info("清理过期消费记录完成，剩余记录数: {}", consumedMessages.size());
    }

    /**
     * 获取当前消费记录数量（用于监控）
     */
    public int getConsumedCount() {
        return consumedMessages.size();
    }
}

