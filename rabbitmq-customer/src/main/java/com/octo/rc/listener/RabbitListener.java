package com.octo.rc.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.stereotype.Component;

import javax.script.Bindings;

@Component
@Slf4j
public class RabbitListener {

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "fanout.queue1")
    public void fanoutQueueMessage(String msg) {
        log.info("fanout.queue1接收到消息了：" + msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "fanout.queue2")
    public void fanoutQueue2Message(String msg) {
        log.info("fanout.queue2接收到消息了：" + msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "direct.queue1")
    public void directQueueMessage(String msg) {
        log.info("direct.queue1接收到消息了：{}", msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "direct.queue2")
    public void directQueue2Message(String msg) {
        log.info("direct.queue2接收到消息了：{}", msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "topic.queue1")
    public void topicQueueMessage(String msg) {
        log.info("topic.queue1接收到消息了：{}", msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "topic.queue2")
    public void topicQueue2Message(String msg) {
        log.info("topic.queue2接收到消息了：{}", msg);
    }


    @org.springframework.amqp.rabbit.annotation.RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "custom.queue"),
            exchange = @Exchange(name = "custom.exchange", type = ExchangeTypes.DIRECT),
            key = "bleu"
    ))
    public void customListener(String msg) throws InterruptedException {
        log.info("custom.queue接收到消息了：{}", msg);
        int a = 1 / 0;
    }


    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "mall.order.queue")
    public void mallOrderListener(String msg) {
        log.info("mall.order.queue收到的消息：{}", msg);
        int a = 1 / 0;
    }






}
