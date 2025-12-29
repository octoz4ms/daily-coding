package com.octo.eum.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 *
 * @author octo
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 登录日志交换机
     */
    public static final String LOGIN_LOG_EXCHANGE = "login.log.exchange";

    /**
     * 登录日志队列
     */
    public static final String LOGIN_LOG_QUEUE = "login.log.queue";

    /**
     * 登录日志路由键
     */
    public static final String LOGIN_LOG_ROUTING_KEY = "login.log";

    /**
     * 用户事件交换机
     */
    public static final String USER_EVENT_EXCHANGE = "user.event.exchange";

    /**
     * 用户创建队列
     */
    public static final String USER_CREATE_QUEUE = "user.create.queue";

    /**
     * 用户创建路由键
     */
    public static final String USER_CREATE_ROUTING_KEY = "user.create";

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        // 开启confirm回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败，记录日志或进行重试
                System.err.println("消息发送失败: " + cause);
            }
        });
        return rabbitTemplate;
    }

    /**
     * 监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // 并发消费者数量
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        // 每次获取消息数量
        factory.setPrefetchCount(10);
        return factory;
    }

    // ==================== 登录日志相关 ====================

    /**
     * 登录日志交换机
     */
    @Bean
    public DirectExchange loginLogExchange() {
        return new DirectExchange(LOGIN_LOG_EXCHANGE, true, false);
    }

    /**
     * 登录日志队列
     */
    @Bean
    public Queue loginLogQueue() {
        return QueueBuilder.durable(LOGIN_LOG_QUEUE)
                // 设置队列的最大长度
                .maxLength(10000L)
                // 消息TTL 7天
                .ttl(7 * 24 * 60 * 60 * 1000)
                .build();
    }

    /**
     * 登录日志绑定
     */
    @Bean
    public Binding loginLogBinding(Queue loginLogQueue, DirectExchange loginLogExchange) {
        return BindingBuilder.bind(loginLogQueue).to(loginLogExchange).with(LOGIN_LOG_ROUTING_KEY);
    }

    // ==================== 用户事件相关 ====================

    /**
     * 用户事件交换机
     */
    @Bean
    public DirectExchange userEventExchange() {
        return new DirectExchange(USER_EVENT_EXCHANGE, true, false);
    }

    /**
     * 用户创建队列
     */
    @Bean
    public Queue userCreateQueue() {
        return QueueBuilder.durable(USER_CREATE_QUEUE).build();
    }

    /**
     * 用户创建绑定
     */
    @Bean
    public Binding userCreateBinding(Queue userCreateQueue, DirectExchange userEventExchange) {
        return BindingBuilder.bind(userCreateQueue).to(userEventExchange).with(USER_CREATE_ROUTING_KEY);
    }
}

