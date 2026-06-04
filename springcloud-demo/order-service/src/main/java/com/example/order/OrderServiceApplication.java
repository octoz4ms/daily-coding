package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 订单服务启动类
 */
@SpringBootApplication
@EnableFeignClients(basePackages = {"com.example.feign"})
@EnableAsync
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Order Service 服务启动成功！");
        System.out.println("========================================");
    }
}
