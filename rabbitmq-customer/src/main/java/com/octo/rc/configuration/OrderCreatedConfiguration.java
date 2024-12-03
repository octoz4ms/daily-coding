package com.octo.rc.configuration;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderCreatedConfiguration {

    @Bean
    public TopicExchange orderCreatedExchange() {
        return ExchangeBuilder.topicExchange("order.created.exchange").build();
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable("order.created.queue").deadLetterExchange("dlx.exchange").deadLetterRoutingKey("dlx").build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderCreatedExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderCreatedExchange).with("#.order.created");
    }

}
