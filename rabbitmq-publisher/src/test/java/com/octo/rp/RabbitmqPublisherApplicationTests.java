package com.octo.rp;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

}
