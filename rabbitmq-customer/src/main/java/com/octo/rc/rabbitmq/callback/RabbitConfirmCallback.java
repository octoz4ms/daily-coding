package com.octo.rc.rabbitmq.callback;

import com.octo.rc.rabbitmq.service.MessageRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * RabbitMQ 生产者确认回调
 * 
 * 实现两个回调：
 * 1. ConfirmCallback - 确认消息是否到达 Exchange
 * 2. ReturnsCallback - 确认消息是否路由到 Queue
 */
@Component
@Slf4j
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    @Autowired
    private RabbitTemplate reliableRabbitTemplate;

    @Autowired
    private MessageRecordService messageRecordService;

    /**
     * 初始化时设置回调
     */
    @PostConstruct
    public void init() {
        reliableRabbitTemplate.setConfirmCallback(this);
        reliableRabbitTemplate.setReturnsCallback(this);
        log.info("RabbitMQ 确认回调初始化完成");
    }

    /**
     * 消息到达 Exchange 的确认回调
     * 
     * @param correlationData 相关数据（包含消息ID）
     * @param ack 是否确认成功
     * @param cause 失败原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String messageId = correlationData != null ? correlationData.getId() : "unknown";
        
        if (ack) {
            // 消息成功到达 Exchange
            log.info("✅ 消息成功到达Exchange，messageId: {}", messageId);
            messageRecordService.markAsSuccess(messageId);
        } else {
            // 消息未能到达 Exchange
            log.error("❌ 消息未能到达Exchange，messageId: {}, cause: {}", messageId, cause);
            messageRecordService.markAsFailed(messageId, "消息未能到达Exchange: " + cause);
            
            // 可以在这里触发重试逻辑
            messageRecordService.incrementRetryCount(messageId);
        }
    }

    /**
     * 消息无法路由到 Queue 的回调
     * 
     * 当 mandatory=true 且消息无法路由到任何队列时触发
     * 
     * @param returned 返回的消息信息
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        String messageId = returned.getMessage().getMessageProperties().getMessageId();
        
        log.error("❌ 消息无法路由到队列！");
        log.error("   messageId: {}", messageId);
        log.error("   exchange: {}", returned.getExchange());
        log.error("   routingKey: {}", returned.getRoutingKey());
        log.error("   replyCode: {}", returned.getReplyCode());
        log.error("   replyText: {}", returned.getReplyText());
        
        // 更新消息状态
        if (messageId != null) {
            messageRecordService.markAsFailed(messageId, 
                String.format("消息无法路由: replyCode=%d, replyText=%s", 
                    returned.getReplyCode(), returned.getReplyText()));
        }
    }
}

