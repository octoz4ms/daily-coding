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

/**
 * 订单超时消费者
 */
@Slf4j
@Component
public class OrderTimeoutConsumer {

    @Autowired
    private OrderService orderService;

    /**
     * 处理订单超时消息
     * 监听死信队列，当消息过期后会自动转发到这里
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_DEAD_QUEUE)
    public void handleOrderTimeout(String orderNo, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        
        log.info("收到订单超时消息，orderNo: {}", orderNo);

        try {
            // 处理订单超时
            orderService.handleOrderTimeout(orderNo);

            // 手动ACK确认
            channel.basicAck(deliveryTag, false);
            log.info("订单超时消息处理成功，orderNo: {}", orderNo);

        } catch (Exception e) {
            log.error("订单超时消息处理失败，orderNo: {}", orderNo, e);
            
            // 判断是否需要重试
            Integer retryCount = (Integer) message.getMessageProperties().getHeaders().get("x-retry-count");
            if (retryCount == null) {
                retryCount = 0;
            }

            if (retryCount < 3) {
                // 重新入队重试
                channel.basicNack(deliveryTag, false, true);
            } else {
                // 超过重试次数，确认消息，记录日志
                log.error("订单超时消息处理失败，已达最大重试次数，orderNo: {}", orderNo);
                channel.basicAck(deliveryTag, false);
                // TODO: 发送告警、记录到异常表等
            }
        }
    }
}

