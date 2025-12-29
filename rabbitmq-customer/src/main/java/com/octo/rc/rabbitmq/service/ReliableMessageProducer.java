package com.octo.rc.rabbitmq.service;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.config.ReliableRabbitConfig;
import com.octo.rc.rabbitmq.entity.MessageRecord;
import com.octo.rc.rabbitmq.entity.ReliableMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 可靠消息生产者
 * 
 * 实现可靠消息发送的完整流程：
 * 1. 创建消息记录（本地消息表）
 * 2. 发送消息到 RabbitMQ
 * 3. 通过回调确认消息状态
 * 4. 失败时支持重发
 */
@Service
@Slf4j
public class ReliableMessageProducer {

    @Autowired
    private RabbitTemplate reliableRabbitTemplate;

    @Autowired
    private MessageRecordService messageRecordService;

    /**
     * 发送可靠消息
     * 
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param data         业务数据
     * @return 消息ID
     */
    public String sendReliableMessage(String businessId, String businessType, Object data) {
        return sendReliableMessage(
                businessId, 
                businessType, 
                data,
                ReliableRabbitConfig.RELIABLE_EXCHANGE,
                ReliableRabbitConfig.RELIABLE_ROUTING_KEY
        );
    }

    /**
     * 发送可靠消息（指定交换机和路由键）
     * 
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @param data         业务数据
     * @param exchange     交换机
     * @param routingKey   路由键
     * @return 消息ID
     */
    public String sendReliableMessage(String businessId, String businessType, Object data,
                                       String exchange, String routingKey) {
        // 1. 构建消息体
        ReliableMessage reliableMessage = ReliableMessage.builder()
                .messageId(UUID.randomUUID().toString().replace("-", ""))
                .businessId(businessId)
                .businessType(businessType)
                .data(data)
                .createTime(LocalDateTime.now())
                .timestamp(System.currentTimeMillis())
                .build();

        String messageBody = JSON.toJSONString(reliableMessage);

        // 2. 创建本地消息记录（生产环境应该在同一个事务中）
        MessageRecord record = messageRecordService.createMessageRecord(
                businessId, businessType, exchange, routingKey, messageBody
        );

        // 3. 发送消息
        doSendMessage(record.getMessageId(), messageBody, exchange, routingKey);

        return record.getMessageId();
    }

    /**
     * 重发消息
     * 
     * @param record 消息记录
     */
    public void resendMessage(MessageRecord record) {
        log.info("重发消息，messageId: {}, retryCount: {}", record.getMessageId(), record.getRetryCount());
        doSendMessage(
                record.getMessageId(),
                record.getMessageBody(),
                record.getExchange(),
                record.getRoutingKey()
        );
    }

    /**
     * 实际发送消息
     */
    private void doSendMessage(String messageId, String messageBody, String exchange, String routingKey) {
        try {
            // 更新状态为发送中
            messageRecordService.markAsSending(messageId);

            // 构建消息属性
            Message message = MessageBuilder
                    .withBody(messageBody.getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding("UTF-8")
                    .setMessageId(messageId)
                    // 消息持久化
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            // 关联数据，用于回调时识别消息
            CorrelationData correlationData = new CorrelationData(messageId);

            // 发送消息
            reliableRabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
            
            log.info("消息发送完成，等待确认，messageId: {}, exchange: {}, routingKey: {}", 
                    messageId, exchange, routingKey);

        } catch (Exception e) {
            log.error("消息发送异常，messageId: {}, error: {}", messageId, e.getMessage(), e);
            messageRecordService.markAsFailed(messageId, "发送异常: " + e.getMessage());
            // 触发重试
            messageRecordService.incrementRetryCount(messageId);
        }
    }

    /**
     * 发送简单消息（不带可靠性保证，用于对比测试）
     */
    public void sendSimpleMessage(String message) {
        reliableRabbitTemplate.convertAndSend(
                ReliableRabbitConfig.RELIABLE_EXCHANGE,
                ReliableRabbitConfig.RELIABLE_ROUTING_KEY,
                message
        );
        log.info("简单消息发送完成: {}", message);
    }
}

