package com.octo.rc.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FanoutConfig {
    @Bean
    public FanoutExchange fanoutExchange(){
        return ExchangeBuilder.fanoutExchange("zms.fanout").build();
    }

    @Bean
    public Queue fanoutQueue1(){
        return QueueBuilder.durable("zms.fanout.queue1").build();
    }

    @Bean
    public Binding binding(Queue fanoutQueue1, FanoutExchange fanoutExchange){
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }


}
