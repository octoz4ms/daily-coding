package com.octo.rc.listener;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RabbitListener {

    @org.springframework.amqp.rabbit.annotation.RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "custom.queue"),
            exchange = @Exchange(name = "custom.exchange", type = ExchangeTypes.DIRECT),
            key = "bleu"
    ))
    public void customListener(String msg) {
        log.info("custom.queue接收到消息了：{}", msg);
        int a = 1 / 0;
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "order.created.queue")
    public void orderCreatedListener(String msg) {
        log.info("order.created.queue接收到消息了：" + msg);
        int a = 1 / 0;
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "email.sending.queue")
    public void  emailSendingListener(String msg) {
        log.info("email.sending.queue接收到消息了：" + msg);
        int a = 1 / 0;
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "order.cancel.queue")
    public void orderCancelListener(String msg) {
        log.info("order.cancel.queue接收到消息了：" + msg);
    }

    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "delay.queue")
    public void delayListener(String msg) {
        log.info("delay.queue接收到消息了：" + msg);
    }

}
