package com.octo.rc.rabbitmq.controller;

import com.octo.rc.rabbitmq.entity.PaymentMessage;
import com.octo.rc.rabbitmq.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付控制器
 * 
 * 测试死信队列：
 * 1. 正常支付（金额 <= 10000）：POST /payment/create?orderId=1&userId=1&amount=100&paymentMethod=ALIPAY
 *    -> 消息正常处理
 * 
 * 2. 异常支付（金额 > 10000）：POST /payment/create?orderId=1&userId=1&amount=20000&paymentMethod=ALIPAY
 *    -> 消息处理失败，转入死信队列
 */
@RestController
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 发起支付
     */
    @PostMapping("/create")
    public PaymentMessage createPayment(
            @RequestParam String orderId,
            @RequestParam String userId,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "ALIPAY") String paymentMethod) {
        return paymentService.createPayment(orderId, userId, amount, paymentMethod);
    }
}










