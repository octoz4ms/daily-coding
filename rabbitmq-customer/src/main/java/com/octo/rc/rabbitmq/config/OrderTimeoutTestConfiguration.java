package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时测试配置 - 使用较短的TTL便于测试
 * 
 * 测试用队列：10秒超时
 */
@Configuration
public class OrderTimeoutTestConfiguration {

    /**
     * 测试用超时时间：10秒
     */
    public static final int TEST_TIMEOUT_MILLIS = 10 * 1000;

    // ==================== 测试用死信交换机和处理队列 ====================

    @Bean
    public DirectExchange orderTestDlxExchange() {
        return ExchangeBuilder.directExchange("order.test.dlx.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderTestProcessQueue() {
        return QueueBuilder.durable("order.test.process.queue").build();
    }

    @Bean
    public Binding orderTestProcessBinding(Queue orderTestProcessQueue, DirectExchange orderTestDlxExchange) {
        return BindingBuilder.bind(orderTestProcessQueue)
                .to(orderTestDlxExchange)
                .with("order.test.timeout.key");
    }

    // ==================== 测试用延迟队列（10秒TTL） ====================

    @Bean
    public DirectExchange orderTestDelayExchange() {
        return ExchangeBuilder.directExchange("order.test.delay.exchange")
                .durable(true)
                .build();
    }

    @Bean
    public Queue orderTestDelayQueue() {
        return QueueBuilder.durable("order.test.delay.queue")
                .ttl(TEST_TIMEOUT_MILLIS)  // 10秒超时
                .deadLetterExchange("order.test.dlx.exchange")
                .deadLetterRoutingKey("order.test.timeout.key")
                .build();
    }

    @Bean
    public Binding orderTestDelayBinding(Queue orderTestDelayQueue, DirectExchange orderTestDelayExchange) {
        return BindingBuilder.bind(orderTestDelayQueue)
                .to(orderTestDelayExchange)
                .with("order.test.delay.key");
    }
}





