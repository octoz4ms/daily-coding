package com.octo.rc.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailSendingConfiguration {

    @Bean
    public DirectExchange emailSendingExchange() {
        return ExchangeBuilder.directExchange("email.sending.exchange").build();
    }

    @Bean
    public Queue emailSendingQueue() {
        return QueueBuilder.durable("email.sending.queue").build();
    }

    @Bean
    public Binding emailSendingBinding(Queue emailSendingQueue, DirectExchange emailSendingExchange) {
        return BindingBuilder.bind(emailSendingQueue).to(emailSendingExchange).with("email.sending");
    }
}
