package com.example.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
public class ApiClientsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiClientsApplication.class, args);
    }

}
