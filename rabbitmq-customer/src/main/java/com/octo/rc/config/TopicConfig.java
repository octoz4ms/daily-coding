package com.octo.rc.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicConfig {

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange("dlx.exchange").build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable("dlx.queue").build();
    }

    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with("dlx.routeKey");
    }

    @Bean
    public TopicExchange mallOrderExchange() {
        return ExchangeBuilder.topicExchange("mall.order.exchange").build();
    }

    @Bean
    public Queue mallOrderQueue() {
        return QueueBuilder.durable("mall.order.queue").deadLetterExchange("dlx.exchange").deadLetterRoutingKey("dlx.routeKey").build();
    }

    @Bean
    public Binding mallOrderBinding(Queue mallOrderQueue, TopicExchange mallOrderExchange) {
        return BindingBuilder.bind(mallOrderQueue).to(mallOrderExchange).with("#.order");
    }
}
