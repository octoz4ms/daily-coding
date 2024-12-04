package com.octo.rc.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderCancelConfiguration {


    @Bean
    public DirectExchange orderCancelExchange() {
        return ExchangeBuilder.directExchange("order.cancel.exchange").build();
    }

    @Bean
    public Queue orderCancelQueue() {
        return QueueBuilder.durable("order.cancel.queue").build();
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, DirectExchange orderCancelExchange) {
        return BindingBuilder.bind(orderCancelQueue).to(orderCancelExchange).with("order.cancel");
    }

    @Bean
    public DirectExchange orderCancelDelayExchange() {
        return ExchangeBuilder.directExchange("order.cancel.delay.exchange").build();
    }

    @Bean
    public Queue orderCancelDelayQueue() {
        return QueueBuilder.durable("order.cancel.delay.queue").deadLetterExchange("order.cancel.exchange").deadLetterRoutingKey("order.cancel").build();
    }

    @Bean
    public Binding orderCancelDelayBinding(Queue orderCancelDelayQueue, DirectExchange orderCancelDelayExchange) {
        return BindingBuilder.bind(orderCancelDelayQueue).to(orderCancelDelayExchange).with("order.cancel.delay");
    }
}
