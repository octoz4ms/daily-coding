package com.octo.seckill.mq;

import com.octo.seckill.config.RabbitMQConfig;
import com.octo.seckill.dto.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀消息生产者
 * 
 * 面试要点：如何保证消息不丢失？
 * 
 * 生产者端：
 * 1. 消息持久化（deliveryMode=2）
 * 2. 发布确认机制（publisher-confirm）
 * 3. 消息返回机制（publisher-return）
 * 
 * Broker端：
 * 1. 持久化队列（durable=true）
 * 2. 持久化交换机
 * 3. 镜像队列（集群模式）
 * 
 * 消费者端：
 * 1. 手动ACK
 * 2. 消费失败重试
 * 3. 死信队列兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀订单消息
     * 
     * @param message 秒杀消息
     */
    public void sendSeckillMessage(SeckillMessage message) {
        log.info("发送秒杀消息: userId={}, activityId={}, orderNo={}", 
                message.getUserId(), message.getActivityId(), message.getOrderNo());
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SECKILL_EXCHANGE,
                RabbitMQConfig.SECKILL_ROUTING_KEY,
                message
        );
    }
}

