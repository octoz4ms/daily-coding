package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时取消配置 - 基于TTL + 死信队列实现
 * 
 * 工作原理：
 * 1. 订单创建时，消息发送到延迟队列（order.delay.queue）
 * 2. 延迟队列设置了TTL和死信交换机
 * 3. 消息在延迟队列中等待TTL时间后过期
 * 4. 过期的消息被转发到死信交换机（order.dlx.exchange）
 * 5. 死信交换机将消息路由到处理队列（order.process.queue）
 * 6. 消费者监听处理队列，执行订单取消逻辑
 */
@Configuration
public class OrderTimeoutDLXConfiguration {

    /**
     * 订单超时时间（单位：毫秒）
     * 生产环境通常设置为30分钟 = 30 * 60 * 1000
     * 为了演示方便，这里设置为10秒
     */
    public static final int ORDER_TIMEOUT_MILLIS = 10 * 1000;

    // ==================== 死信交换机和处理队列 ====================

    /**
     * 死信交换机 - 接收延迟队列中过期的消息
     */
    @Bean
    public DirectExchange orderDlxExchange() {
        return ExchangeBuilder.directExchange("order.dlx.exchange")
                .durable(true)
                .build();
    }

    /**
     * 订单处理队列 - 消费者监听此队列来处理超时订单
     */
    @Bean
    public Queue orderProcessQueue() {
        return QueueBuilder.durable("order.process.queue").build();
    }

    /**
     * 绑定处理队列到死信交换机
     */
    @Bean
    public Binding orderProcessBinding(Queue orderProcessQueue, DirectExchange orderDlxExchange) {
        return BindingBuilder.bind(orderProcessQueue)
                .to(orderDlxExchange)
                .with("order.timeout.routing.key");
    }

    // ==================== 延迟队列（带TTL） ====================

    /**
     * 订单延迟交换机 - 接收新创建的订单消息
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder.directExchange("order.delay.exchange")
                .durable(true)
                .build();
    }

    /**
     * 订单延迟队列 - 消息在此队列等待TTL时间后转发到死信交换机
     * 
     * 关键配置：
     * - x-message-ttl: 消息过期时间
     * - x-dead-letter-exchange: 死信交换机
     * - x-dead-letter-routing-key: 死信路由键
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable("order.delay.queue")
                // 设置队列消息TTL（毫秒）
                .ttl(ORDER_TIMEOUT_MILLIS)
                // 设置死信交换机
                .deadLetterExchange("order.dlx.exchange")
                // 设置死信路由键
                .deadLetterRoutingKey("order.timeout.routing.key")
                .build();
    }

    /**
     * 绑定延迟队列到延迟交换机
     */
    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with("order.delay.routing.key");
    }
}


