package com.example.payment.mq;

import com.example.payment.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付消息生产者
 */
@Slf4j
@Component
public class PaymentMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送支付成功消息
     *
     * @param orderNo   订单号
     * @param paymentNo 支付单号
     */
    public void sendPaySuccessMessage(String orderNo, String paymentNo) {
        log.info("发送支付成功消息，orderNo: {}, paymentNo: {}", orderNo, paymentNo);

        Map<String, String> message = new HashMap<>();
        message.put("orderNo", orderNo);
        message.put("paymentNo", paymentNo);
        message.put("timestamp", String.valueOf(System.currentTimeMillis()));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_SUCCESS_EXCHANGE,
                RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
                message
        );

        log.info("支付成功消息发送成功，orderNo: {}", orderNo);
    }
}

