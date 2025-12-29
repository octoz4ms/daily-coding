package com.octo.rc.rabbitmq.listener;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.config.ReliableRabbitConfig;
import com.octo.rc.rabbitmq.entity.ReliableMessage;
import com.octo.rc.rabbitmq.service.IdempotentService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 可靠消息消费者
 * 
 * 实现消费者端的可靠性：
 * 1. 手动确认模式 (Manual ACK)
 * 2. 消息幂等性检查
 * 3. 业务处理失败转入死信队列
 */
@Component
@Slf4j
public class ReliableMessageConsumer {

    @Autowired
    private IdempotentService idempotentService;

    /**
     * 消费业务队列消息
     * 
     * 使用手动确认模式，确保消息被正确处理
     */
    @RabbitListener(queues = ReliableRabbitConfig.RELIABLE_QUEUE, ackMode = "MANUAL")
    public void handleMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String body = new String(message.getBody());

        log.info("📩 收到消息，messageId: {}, deliveryTag: {}", messageId, deliveryTag);

        try {
            // 1. 幂等性检查
            if (!idempotentService.tryAcquire(messageId)) {
                log.warn("⚠️ 消息已被消费，跳过处理，messageId: {}", messageId);
                // 直接确认，不重复处理
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 解析消息
            ReliableMessage reliableMessage = JSON.parseObject(body, ReliableMessage.class);
            log.info("📋 消息内容: businessId={}, businessType={}, data={}", 
                    reliableMessage.getBusinessId(),
                    reliableMessage.getBusinessType(),
                    reliableMessage.getData());

            // 3. 业务处理
            processBusinessLogic(reliableMessage);

            // 4. 处理成功，确认消息
            channel.basicAck(deliveryTag, false);
            log.info("✅ 消息处理成功，messageId: {}", messageId);

        } catch (BusinessException e) {
            // 业务异常，不重试，直接转入死信队列
            log.error("❌ 业务处理失败(不重试)，转入死信队列，messageId: {}, error: {}", messageId, e.getMessage());
            idempotentService.release(messageId);
            // requeue=false，消息将转入死信队列
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception e) {
            // 其他异常，可以选择重试
            log.error("❌ 消息处理异常，messageId: {}, error: {}", messageId, e.getMessage(), e);
            idempotentService.release(messageId);
            
            // 判断是否需要重试
            Integer retryCount = getRetryCount(message);
            if (retryCount < 3) {
                // 重新入队，触发重试
                log.warn("⏳ 消息将重新入队重试，messageId: {}, retryCount: {}", messageId, retryCount);
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过重试次数，转入死信队列
                log.error("❌ 重试次数超限，转入死信队列，messageId: {}", messageId);
                channel.basicNack(deliveryTag, false, false);
            }
        }
    }

    /**
     * 消费死信队列消息
     * 
     * 死信消息的处理策略：
     * 1. 记录日志和监控
     * 2. 发送告警通知
     * 3. 人工干预处理
     */
    @RabbitListener(queues = ReliableRabbitConfig.RELIABLE_DLX_QUEUE)
    public void handleDeadLetter(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String body = new String(message.getBody());

        log.error("☠️ 【死信队列】收到死信消息，messageId: {}", messageId);
        log.error("☠️ 【死信队列】消息内容: {}", body);

        // 获取死信原因
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        log.error("☠️ 【死信队列】死信原因: {}", xDeath);

        try {
            ReliableMessage reliableMessage = JSON.parseObject(body, ReliableMessage.class);
            
            // 记录异常日志（生产环境存入数据库）
            log.error("☠️ 【死信队列】业务信息: businessId={}, businessType={}", 
                    reliableMessage.getBusinessId(), reliableMessage.getBusinessType());

            // 发送告警（邮件、短信、钉钉等）
            sendAlert(reliableMessage);

            // 可选：将消息存入专门的异常表，便于后续人工处理
            saveToExceptionTable(reliableMessage);

        } catch (Exception e) {
            log.error("☠️ 【死信队列】处理死信消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 业务处理逻辑
     */
    private void processBusinessLogic(ReliableMessage message) {
        // 模拟业务处理
        log.info("🔄 开始处理业务逻辑...");
        
        // 模拟处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 模拟业务异常（当 businessId 以 "error" 开头时）
        if (message.getBusinessId() != null && message.getBusinessId().startsWith("error")) {
            throw new BusinessException("业务处理失败：" + message.getBusinessId());
        }

        // 模拟随机异常（用于测试重试机制）
        if (message.getBusinessId() != null && message.getBusinessId().startsWith("random")) {
            if (Math.random() > 0.5) {
                throw new RuntimeException("随机异常，用于测试重试机制");
            }
        }

        log.info("🔄 业务处理完成");
    }

    /**
     * 获取消息重试次数
     */
    private Integer getRetryCount(Message message) {
        Object retryCount = message.getMessageProperties().getHeaders().get("x-retry-count");
        return retryCount == null ? 0 : (Integer) retryCount;
    }

    /**
     * 发送告警
     */
    private void sendAlert(ReliableMessage message) {
        log.warn("🚨 【告警】消息处理失败，需要人工处理！");
        log.warn("🚨 【告警】businessId: {}, businessType: {}", 
                message.getBusinessId(), message.getBusinessType());
        // 实际项目中调用告警接口
    }

    /**
     * 保存到异常表
     */
    private void saveToExceptionTable(ReliableMessage message) {
        log.info("💾 保存异常消息到数据库，便于后续处理");
        // 实际项目中保存到数据库
    }

    /**
     * 业务异常（不需要重试的异常）
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}

