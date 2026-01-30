package com.example.payment.mq;

import com.example.payment.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者
 */
@Slf4j
@Component
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时延迟消息
     *
     * @param orderNo        订单号
     * @param delayMinutes   延迟分钟数
     */
    public void sendOrderTimeoutMessage(String orderNo, int delayMinutes) {
        log.info("发送订单超时延迟消息，orderNo: {}, delayMinutes: {}", orderNo, delayMinutes);

        // 设置消息过期时间（毫秒）
        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setExpiration(String.valueOf(delayMinutes * 60 * 1000));
            return message;
        };

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                RabbitMQConfig.ORDER_TIMEOUT_ROUTING_KEY,
                orderNo,
                messagePostProcessor
        );

        log.info("订单超时延迟消息发送成功，orderNo: {}", orderNo);
    }
}

