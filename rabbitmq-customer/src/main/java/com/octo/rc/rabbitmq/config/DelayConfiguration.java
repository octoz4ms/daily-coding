package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayConfiguration {

    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder.directExchange("delay.exchange").delayed().build();
    }

    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable("delay.queue").build();
    }

    @Bean
    public Binding delayBinding(Queue delayQueue, DirectExchange delayExchange) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with("simple.delay");
    }
}
