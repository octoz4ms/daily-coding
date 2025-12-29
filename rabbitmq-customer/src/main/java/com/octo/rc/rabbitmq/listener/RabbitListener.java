package com.octo.rc.rabbitmq.listener;


import com.octo.rc.rabbitmq.service.OrderDLXService;
import com.octo.rc.rabbitmq.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RabbitListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDLXService orderDLXService;

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

    // ==================== 订单超时取消监听器 ====================

    /**
     * 监听订单超时队列 - 基于延迟交换机方式
     * 需要 RabbitMQ 安装 rabbitmq-delayed-message-exchange 插件
     * 
     * @param orderId 订单ID
     */
    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "order.timeout.queue")
    public void orderTimeoutListener(String orderId) {
        log.info("【延迟交换机方式】收到订单超时消息，订单ID: {}", orderId);
        boolean success = orderService.cancelOrder(orderId);
        if (success) {
            log.info("【延迟交换机方式】订单超时取消成功，订单ID: {}", orderId);
        } else {
            log.info("【延迟交换机方式】订单无需取消（可能已支付或已取消），订单ID: {}", orderId);
        }
    }

    /**
     * 监听订单处理队列 - 基于 TTL + 死信队列方式
     * 处理从延迟队列过期后转发过来的消息
     * 
     * @param orderId 订单ID
     */
    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = "order.process.queue")
    public void orderProcessListener(String orderId) {
        log.info("【DLX方式】收到订单超时消息，订单ID: {}", orderId);
        boolean success = orderDLXService.handleTimeoutOrder(orderId);
        if (success) {
            log.info("【DLX方式】订单超时取消成功，订单ID: {}", orderId);
        } else {
            log.info("【DLX方式】订单无需取消（可能已支付或已取消），订单ID: {}", orderId);
        }
    }
}
