package com.example.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单支付系统启动类
 *
 * @author example
 */
@SpringBootApplication
@MapperScan("com.example.payment.mapper")
@EnableScheduling
public class OrderPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPaymentApplication.class, args);
        System.out.println("============================================");
        System.out.println("  订单支付系统启动成功！");
        System.out.println("  API地址: http://localhost:8089");
        System.out.println("  商品列表: http://localhost:8089/api/product/list");
        System.out.println("============================================");
    }
}

