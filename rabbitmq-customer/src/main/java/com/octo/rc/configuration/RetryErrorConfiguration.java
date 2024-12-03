package com.octo.rc.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryErrorConfiguration {

    @Bean
    public DirectExchange retryErrorExchange() {
      return  ExchangeBuilder.directExchange("retry.error.exchange").build();
    }

    @Bean
    public Queue retryErrorQueue() {
        return QueueBuilder.durable("retry.error.queue").build();
    }

    @Bean
    public Binding errorBinding(Queue retryErrorQueue, DirectExchange retryErrorExchange) {
        return BindingBuilder.bind(retryErrorQueue).to(retryErrorExchange).with("retry.error");
    }

    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, "retry.error.exchange","retry.error");
    }
}
