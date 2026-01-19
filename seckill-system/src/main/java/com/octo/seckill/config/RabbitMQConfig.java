package com.octo.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置
 * 
 * 消息队列削峰原理：
 * 1. 前端请求 -> 预扣减Redis库存 -> 发送MQ消息 -> 立即返回"排队中"
 * 2. 消费者异步消费消息 -> 创建订单 -> 扣减数据库库存
 * 
 * 面试要点：
 * - 为什么用MQ削峰？
 *   高并发时数据库扛不住，MQ作为缓冲，消费者匀速消费
 * - 消息丢失怎么办？
 *   1. 生产者确认机制（publisher-confirm）
 *   2. 消息持久化（durable queue）
 *   3. 消费者手动ACK
 */
@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    // 死信队列 - 处理失败的消息
    public static final String SECKILL_DLX_EXCHANGE = "seckill.dlx.exchange";
    public static final String SECKILL_DLX_QUEUE = "seckill.dlx.queue";
    public static final String SECKILL_DLX_ROUTING_KEY = "seckill.dlx";

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        
        // 消息发送确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 发送失败，需要重试或记录日志
                System.err.println("消息发送失败: " + cause);
            }
        });
        
        // 消息返回回调（无法路由时）
        template.setReturnsCallback(returned -> {
            System.err.println("消息无法路由: " + returned.getMessage());
        });
        
        return template;
    }

    // ================== 秒杀订单队列 ==================

    @Bean
    public DirectExchange seckillExchange() {
        return ExchangeBuilder.directExchange(SECKILL_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                // 绑定死信交换机
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, DirectExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue)
                .to(seckillExchange)
                .with(SECKILL_ROUTING_KEY);
    }

    // ================== 死信队列 ==================

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(SECKILL_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(SECKILL_DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue)
                .to(dlxExchange)
                .with(SECKILL_DLX_ROUTING_KEY);
    }
}

