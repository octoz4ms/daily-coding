package com.octo.demo.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务提供者启动类
 * <p>
 * 提供用户相关的 RESTful API，供其他服务调用
 */
@SpringBootApplication
public class ProviderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderServiceApplication.class, args);
    }
}

