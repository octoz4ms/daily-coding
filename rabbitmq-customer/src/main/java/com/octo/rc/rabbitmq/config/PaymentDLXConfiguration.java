package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块死信队列配置
 * 
 * 用途：消费失败的消息转入死信队列，用于：
 * 1. 异常消息存档，便于排查问题
 * 2. 人工处理或重新投递
 * 3. 监控告警
 * 
 * 流程：
 * payment.queue (消费失败/拒绝) → payment.dlx.exchange → payment.dlx.queue → 人工处理
 */
@Configuration
public class PaymentDLXConfiguration {

    // ==================== 死信交换机和队列 ====================

    /**
     * 支付死信交换机
     */
    @Bean
    public DirectExchange paymentDlxExchange() {
        return ExchangeBuilder.directExchange("payment.dlx.exchange")
                .durable(true)
                .build();
    }

    /**
     * 支付死信队列 - 存放消费失败的消息
     */
    @Bean
    public Queue paymentDlxQueue() {
        return QueueBuilder.durable("payment.dlx.queue").build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding paymentDlxBinding() {
        return BindingBuilder.bind(paymentDlxQueue())
                .to(paymentDlxExchange())
                .with("payment.dlx.routing.key");
    }

    // ==================== 业务队列（配置死信转发） ====================

    /**
     * 支付业务交换机
     */
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder.directExchange("payment.exchange")
                .durable(true)
                .build();
    }

    /**
     * 支付业务队列 - 配置死信转发
     * 
     * 当消息被拒绝(reject/nack)且requeue=false时，消息将转发到死信交换机
     */
    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable("payment.queue")
                // 指定死信交换机
                .deadLetterExchange("payment.dlx.exchange")
                // 指定死信路由键
                .deadLetterRoutingKey("payment.dlx.routing.key")
                .build();
    }

    /**
     * 绑定业务队列到业务交换机
     */
    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(paymentExchange())
                .with("payment.routing.key");
    }
}









