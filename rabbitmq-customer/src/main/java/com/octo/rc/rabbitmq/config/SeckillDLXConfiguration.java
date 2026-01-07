package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 秒杀模块死信队列配置
 * 
 * 用途：队列限流/削峰
 * 
 * 场景：秒杀活动时请求量巨大，通过设置队列最大长度进行限流
 *      超出限制的消息转入死信队列，可以返回"活动太火爆"提示
 * 
 * 流程：
 * seckill.queue (队列满) → seckill.dlx.exchange → seckill.dlx.queue → 返回排队失败
 */
@Configuration
public class SeckillDLXConfiguration {

    /**
     * 秒杀队列最大长度（限流阈值）
     */
    private static final int MAX_QUEUE_LENGTH = 100;

    // ==================== 死信交换机和队列 ====================

    @Bean
    public DirectExchange seckillDlxExchange() {
        return ExchangeBuilder.directExchange("seckill.dlx.exchange")
                .durable(true)
                .build();
    }

    /**
     * 秒杀死信队列 - 存放被拒绝的秒杀请求
     */
    @Bean
    public Queue seckillDlxQueue() {
        return QueueBuilder.durable("seckill.dlx.queue").build();
    }

    @Bean
    public Binding seckillDlxBinding() {
        return BindingBuilder.bind(seckillDlxQueue())
                .to(seckillDlxExchange())
                .with("seckill.dlx.routing.key");
    }

    // ==================== 业务队列（配置最大长度和死信转发） ====================

    @Bean
    public DirectExchange seckillExchange() {
        return ExchangeBuilder.directExchange("seckill.exchange")
                .durable(true)
                .build();
    }

    /**
     * 秒杀业务队列 - 配置最大长度限流
     * 
     * 当队列消息数量达到 MAX_QUEUE_LENGTH 时，新消息将被转发到死信队列
     */
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable("seckill.queue")
                // 设置队列最大长度
                .maxLength(MAX_QUEUE_LENGTH)
                // 溢出行为：reject-publish-dlx（拒绝并转入死信）
                .overflow(QueueBuilder.Overflow.rejectPublishDlx)
                // 指定死信交换机
                .deadLetterExchange("seckill.dlx.exchange")
                // 指定死信路由键
                .deadLetterRoutingKey("seckill.dlx.routing.key")
                .build();
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with("seckill.routing.key");
    }
}






