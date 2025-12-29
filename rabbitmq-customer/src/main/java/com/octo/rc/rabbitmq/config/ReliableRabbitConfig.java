package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 可靠性配置
 * 
 * 实现消息可靠性的三个层面：
 * 1. 生产者确认 - Publisher Confirm + Publisher Return
 * 2. 消息持久化 - Exchange、Queue、Message 持久化
 * 3. 消费者确认 - 手动ACK
 */
@Configuration
public class ReliableRabbitConfig {

    // ==================== Exchange 名称 ====================
    public static final String RELIABLE_EXCHANGE = "reliable.direct.exchange";
    public static final String RELIABLE_DLX_EXCHANGE = "reliable.dlx.exchange";
    
    // ==================== Queue 名称 ====================
    public static final String RELIABLE_QUEUE = "reliable.queue";
    public static final String RELIABLE_DLX_QUEUE = "reliable.dlx.queue";
    
    // ==================== Routing Key ====================
    public static final String RELIABLE_ROUTING_KEY = "reliable.message";
    public static final String RELIABLE_DLX_ROUTING_KEY = "reliable.dlx";

    /**
     * 配置 RabbitTemplate，启用生产者确认机制
     */
    @Bean
    public RabbitTemplate reliableRabbitTemplate(ConnectionFactory connectionFactory,
                                                  MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        
        // 开启 mandatory 模式，消息无法路由时会触发 ReturnCallback
        rabbitTemplate.setMandatory(true);
        
        return rabbitTemplate;
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==================== 业务交换机和队列 ====================
    
    /**
     * 业务交换机 - 持久化
     */
    @Bean
    public DirectExchange reliableExchange() {
        // durable = true 持久化交换机
        return ExchangeBuilder
                .directExchange(RELIABLE_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 业务队列 - 持久化，绑定死信交换机
     */
    @Bean
    public Queue reliableQueue() {
        return QueueBuilder
                .durable(RELIABLE_QUEUE)
                // 绑定死信交换机
                .deadLetterExchange(RELIABLE_DLX_EXCHANGE)
                .deadLetterRoutingKey(RELIABLE_DLX_ROUTING_KEY)
                // 消息TTL（可选，这里设置30秒）
                .ttl(30000)
                .build();
    }

    /**
     * 绑定业务队列到业务交换机
     */
    @Bean
    public Binding reliableBinding() {
        return BindingBuilder
                .bind(reliableQueue())
                .to(reliableExchange())
                .with(RELIABLE_ROUTING_KEY);
    }

    // ==================== 死信交换机和队列 ====================

    /**
     * 死信交换机 - 持久化
     */
    @Bean
    public DirectExchange reliableDlxExchange() {
        return ExchangeBuilder
                .directExchange(RELIABLE_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信队列 - 持久化
     */
    @Bean
    public Queue reliableDlxQueue() {
        return QueueBuilder
                .durable(RELIABLE_DLX_QUEUE)
                .build();
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding reliableDlxBinding() {
        return BindingBuilder
                .bind(reliableDlxQueue())
                .to(reliableDlxExchange())
                .with(RELIABLE_DLX_ROUTING_KEY);
    }
}

