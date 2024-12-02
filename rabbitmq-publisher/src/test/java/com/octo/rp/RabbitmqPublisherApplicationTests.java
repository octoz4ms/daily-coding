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
    void sendMessage() {
        String exchangeName = "mall.order.exchange";
        String msg = "用户张名帅在商场下单了！";
        rabbitTemplate.convertAndSend(exchangeName, "zms.order", msg);
    }

}
