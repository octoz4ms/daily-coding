package com.octo.rc.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("zms.direct");
    }

    @Bean
    public Queue queue1() {
        return new Queue("zms.direct.queue");
    }

    @Bean
    public Binding directExchangeBindingQueue1(Queue queue1, DirectExchange directExchange) {
        return BindingBuilder.bind(queue1).to(directExchange).with("red");
    }
}
