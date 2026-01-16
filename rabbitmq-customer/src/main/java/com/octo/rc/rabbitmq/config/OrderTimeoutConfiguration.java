package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时取消配置
 * 使用延迟交换机实现订单超时自动取消
 */
@Configuration
public class OrderTimeoutConfiguration {

    /**
     * 订单超时延迟交换机
     * 使用 delayed() 方法创建延迟交换机，需要 RabbitMQ 安装 rabbitmq-delayed-message-exchange 插件
     */
    @Bean
    public DirectExchange orderTimeoutExchange() {
        return ExchangeBuilder.directExchange("order.timeout.exchange")
                .delayed()
                .durable(true)
                .build();
    }

    /**
     * 订单超时取消队列
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable("order.timeout.queue").build();
    }

    /**
     * 绑定订单超时队列到延迟交换机
     */
    @Bean
    public Binding orderTimeoutBinding(Queue orderTimeoutQueue, DirectExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutQueue)
                .to(orderTimeoutExchange)
                .with("order.timeout");
    }
}











