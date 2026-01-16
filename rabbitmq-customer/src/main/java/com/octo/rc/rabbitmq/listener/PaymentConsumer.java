package com.octo.rc.rabbitmq.listener;

import com.alibaba.fastjson.JSON;
import com.octo.rc.rabbitmq.entity.PaymentMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 支付消息消费者
 * 
 * 演示消费失败时如何将消息转入死信队列
 */
@Component
@Slf4j
public class PaymentConsumer {

    /**
     * 处理支付消息
     * 
     * 使用手动确认模式，处理失败时拒绝消息(不重新入队)，消息将转入死信队列
     */
    @RabbitListener(queues = "payment.queue", ackMode = "MANUAL")
    public void handlePayment(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        
        try {
            log.info("收到支付消息: {}", body);
            PaymentMessage paymentMessage = JSON.parseObject(body, PaymentMessage.class);
            
            // 模拟业务处理
            processPayment(paymentMessage);
            
            // 处理成功，确认消息
            channel.basicAck(deliveryTag, false);
            log.info("支付处理成功，paymentId: {}", paymentMessage.getPaymentId());
            
        } catch (Exception e) {
            log.error("支付处理失败: {}", e.getMessage());
            
            // 处理失败，拒绝消息且不重新入队
            // requeue=false 时，消息将转入死信队列
            channel.basicNack(deliveryTag, false, false);
            log.warn("消息已转入死信队列，等待人工处理");
        }
    }

    /**
     * 模拟支付处理
     * 金额大于10000时模拟处理失败
     */
    private void processPayment(PaymentMessage message) {
        if (message.getAmount().doubleValue() > 10000) {
            throw new RuntimeException("支付金额超过限额，需要人工审核");
        }
        // 正常业务逻辑...
        log.info("调用支付网关处理支付，金额: {}", message.getAmount());
    }

    /**
     * 处理死信队列中的消息
     * 
     * 死信队列的消费者通常用于：
     * 1. 记录异常日志
     * 2. 发送告警通知
     * 3. 人工处理或重新投递
     */
    @RabbitListener(queues = "payment.dlx.queue")
    public void handleDeadLetter(Message message) {
        String body = new String(message.getBody());
        log.error("【死信队列】收到异常消息: {}", body);
        
        // 获取死信相关信息
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        log.error("【死信队列】死信原因: {}", xDeath);
        
        // 这里可以：
        // 1. 存入数据库，便于后续查询和处理
        // 2. 发送告警通知（邮件、短信、钉钉等）
        // 3. 根据业务需要决定是否重新投递
        
        PaymentMessage paymentMessage = JSON.parseObject(body, PaymentMessage.class);
        log.error("【死信队列】异常支付单号: {}, 用户: {}, 金额: {}", 
                paymentMessage.getPaymentId(), 
                paymentMessage.getUserId(), 
                paymentMessage.getAmount());
        
        // 示例：发送告警
        sendAlertNotification(paymentMessage);
    }

    /**
     * 发送告警通知
     */
    private void sendAlertNotification(PaymentMessage message) {
        log.warn("【告警】支付处理异常，请及时处理！支付单号: {}", message.getPaymentId());
        // 实际项目中可以调用邮件/短信/钉钉接口
    }
}









