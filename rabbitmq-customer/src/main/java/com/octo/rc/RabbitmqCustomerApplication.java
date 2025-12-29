package com.octo.rc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务，用于消息重发
public class RabbitmqCustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqCustomerApplication.class, args);
    }

}
