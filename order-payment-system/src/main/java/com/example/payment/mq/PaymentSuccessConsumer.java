package com.example.payment.mq;

import com.example.payment.config.RabbitMQConfig;
import com.example.payment.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 支付成功消费者
 */
@Slf4j
@Component
public class PaymentSuccessConsumer {

    @Autowired
    private OrderService orderService;

    /**
     * 处理支付成功消息
     */
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(Map<String, String> messageBody, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        String orderNo = messageBody.get("orderNo");
        String paymentNo = messageBody.get("paymentNo");

        log.info("收到支付成功消息，orderNo: {}, paymentNo: {}", orderNo, paymentNo);

        try {
            // 处理支付成功逻辑
            boolean success = orderService.handlePaySuccess(orderNo);

            if (success) {
                // 手动ACK确认
                channel.basicAck(deliveryTag, false);
                log.info("支付成功消息处理完成，orderNo: {}", orderNo);
            } else {
                // 处理失败，可能是订单状态已变更
                log.warn("订单支付处理返回失败，可能已被处理，orderNo: {}", orderNo);
                channel.basicAck(deliveryTag, false);
            }

        } catch (Exception e) {
            log.error("支付成功消息处理异常，orderNo: {}", orderNo, e);

            // 获取重试次数
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) {
                retryCount = 0;
            }

            if (retryCount < 3) {
                // 重新入队重试
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过重试次数
                log.error("支付成功消息处理失败，已达最大重试次数，orderNo: {}", orderNo);
                channel.basicAck(deliveryTag, false);
                // TODO: 记录到异常表，人工处理
            }
        }
    }
}

