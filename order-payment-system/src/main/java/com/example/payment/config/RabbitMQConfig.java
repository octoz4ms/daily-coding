package com.example.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    // ===================== 订单超时相关 =====================
    
    /**
     * 订单超时延迟交换机（使用死信队列实现延迟）
     */
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    
    /**
     * 订单超时延迟队列（消息先发到这里）
     */
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    
    /**
     * 订单超时死信交换机
     */
    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    
    /**
     * 订单超时死信队列（超时后消息转发到这里）
     */
    public static final String ORDER_DEAD_QUEUE = "order.dead.queue";
    
    /**
     * 订单超时路由键
     */
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";

    // ===================== 支付成功相关 =====================
    
    /**
     * 支付成功交换机
     */
    public static final String PAYMENT_SUCCESS_EXCHANGE = "payment.success.exchange";
    
    /**
     * 支付成功队列
     */
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success.queue";
    
    /**
     * 支付成功路由键
     */
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";

    // ===================== 消息转换器 =====================
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        
        // 消息发送确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        
        // 消息返回（路由失败）
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息路由失败: " + returned.getMessage());
        });
        
        return rabbitTemplate;
    }

    // ===================== 订单超时队列声明 =====================

    /**
     * 订单延迟队列（死信队列配置）
     */
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 死信交换机
        args.put("x-dead-letter-exchange", ORDER_DEAD_EXCHANGE);
        // 死信路由键
        args.put("x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTING_KEY);
        return QueueBuilder.durable(ORDER_DELAY_QUEUE).withArguments(args).build();
    }

    /**
     * 订单延迟交换机
     */
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 绑定订单延迟队列到延迟交换机
     */
    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    /**
     * 订单死信队列
     */
    @Bean
    public Queue orderDeadQueue() {
        return QueueBuilder.durable(ORDER_DEAD_QUEUE).build();
    }

    /**
     * 订单死信交换机
     */
    @Bean
    public DirectExchange orderDeadExchange() {
        return new DirectExchange(ORDER_DEAD_EXCHANGE, true, false);
    }

    /**
     * 绑定订单死信队列到死信交换机
     */
    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDeadQueue())
                .to(orderDeadExchange())
                .with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    // ===================== 支付成功队列声明 =====================

    /**
     * 支付成功队列
     */
    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build();
    }

    /**
     * 支付成功交换机
     */
    @Bean
    public DirectExchange paymentSuccessExchange() {
        return new DirectExchange(PAYMENT_SUCCESS_EXCHANGE, true, false);
    }

    /**
     * 绑定支付成功队列到交换机
     */
    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentSuccessExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }
}

