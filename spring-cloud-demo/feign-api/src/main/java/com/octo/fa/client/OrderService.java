package com.octo.fa.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "order-service", fallback = OrderServiceFallback.class, configuration = FeignConfiguration.class)
public interface OrderService {
    @RequestMapping(value = "/order", method = RequestMethod.GET)
    String getOrder();
}

class FeignConfiguration {
    @Bean
    public OrderServiceFallback echoServiceFallback() {
        return new OrderServiceFallback();
    }
}

class OrderServiceFallback implements OrderService {
    @Override
    public String getOrder() {
        return "echo fallback";
    }
}