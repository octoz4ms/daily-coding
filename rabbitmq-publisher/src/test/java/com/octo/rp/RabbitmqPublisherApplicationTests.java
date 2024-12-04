package com.octo.rp;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;

@SpringBootTest
class RabbitmqPublisherApplicationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void createOrder() {
        String exchangeName = "order.created.exchange";
        String msg = "He bought some daily necessities.";
        rabbitTemplate.convertAndSend(exchangeName, "zms.order.created", msg);
    }

    @Test
    void sendMessage() {
        String exchangeName = "email.sending.exchange";
        String msg = "hello, welcome to octo!";
        rabbitTemplate.convertAndSend(exchangeName, "email.sending", msg);
    }

    @Test
    void orderCancel() {
        String exchangeName = "order.cancel.delay.exchange";
        String message = "订单取消了！";
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setExpiration(String.valueOf(5000)); // 设置消息过期时间
        Message msg = new Message(message.getBytes(), messageProperties);
        rabbitTemplate.convertAndSend(exchangeName, "order.cancel.delay", msg);
        System.out.println("消息发送成功!");
    }

    @Test
    void delay() {
        rabbitTemplate.convertAndSend("delay.exchange", "simple.delay", "你好", message -> {
            message.getMessageProperties().setDelay(3000);
            return message;
        });
    }



}
