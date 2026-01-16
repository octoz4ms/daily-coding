package com.octo.rc.rabbitmq.service;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.entity.PaymentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务
 */
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发起支付
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param amount 支付金额
     * @param paymentMethod 支付方式
     * @return 支付消息
     */
    public PaymentMessage createPayment(String orderId, String userId, 
                                        BigDecimal amount, String paymentMethod) {
        // 创建支付单
        PaymentMessage payment = PaymentMessage.builder()
                .paymentId(UUID.randomUUID().toString().replace("-", ""))
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();

        // 发送支付消息到队列
        String messageJson = JSON.toJSONString(payment);
        rabbitTemplate.convertAndSend(
                "payment.exchange",
                "payment.routing.key",
                messageJson
        );

        log.info("支付消息已发送，paymentId: {}, 金额: {}", payment.getPaymentId(), amount);
        return payment;
    }
}









