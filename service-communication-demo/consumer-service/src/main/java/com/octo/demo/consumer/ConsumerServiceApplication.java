package com.octo.demo.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 服务消费者启动类
 * <p>
 * 【标准规范】使用 @EnableFeignClients 启用 Feign 客户端
 * <p>
 * basePackages: 指定 Feign 客户端接口所在的包路径
 * 如果 Feign 接口在其他模块，必须显式指定 basePackages
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.octo.demo.common.client")
public class ConsumerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerServiceApplication.class, args);
    }
}

