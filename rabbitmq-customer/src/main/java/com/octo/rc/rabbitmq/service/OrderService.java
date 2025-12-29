package com.octo.rc.rabbitmq.service;

import com.octo.rc.rabbitmq.entity.Order;
import com.octo.rc.rabbitmq.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单服务类
 */
@Service
@Slf4j
public class OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 模拟订单存储（实际项目中应该使用数据库）
     */
    private final Map<String, Order> orderStorage = new ConcurrentHashMap<>();

    /**
     * 订单超时时间（单位：毫秒）
     * 生产环境通常设置为30分钟 = 30 * 60 * 1000
     * 为了演示方便，这里设置为10秒
     */
    private static final long ORDER_TIMEOUT_MILLIS = 10 * 1000;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param amount      订单金额
     * @return 订单对象
     */
    public Order createOrder(String userId, String productId, String productName, BigDecimal amount) {
        // 生成订单ID
        String orderId = UUID.randomUUID().toString().replace("-", "");

        // 创建订单
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(productName);
        order.setAmount(amount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreateTime(LocalDateTime.now());

        // 保存订单
        orderStorage.put(orderId, order);
        log.info("创建订单成功，订单ID: {}, 用户ID: {}, 商品: {}, 金额: {}", orderId, userId, productName, amount);

        // 发送延迟消息，用于订单超时自动取消
        sendOrderTimeoutMessage(orderId);

        return order;
    }

    /**
     * 发送订单超时延迟消息
     *
     * @param orderId 订单ID
     */
    private void sendOrderTimeoutMessage(String orderId) {
        // 发送延迟消息到订单超时交换机
        // 延迟时间为 ORDER_TIMEOUT_MILLIS 毫秒
        rabbitTemplate.convertAndSend(
                "order.timeout.exchange",
                "order.timeout",
                orderId,
                message -> {
                    // 设置延迟时间（单位：毫秒）
                    message.getMessageProperties().setDelay((int) ORDER_TIMEOUT_MILLIS);
                    return message;
                }
        );
        log.info("发送订单超时延迟消息，订单ID: {}, 超时时间: {} 秒", orderId, ORDER_TIMEOUT_MILLIS / 1000);
    }

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 是否取消成功
     */
    public boolean cancelOrder(String orderId) {
        Order order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("订单不存在，订单ID: {}", orderId);
            return false;
        }

        // 只有待支付状态的订单才能取消
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("订单状态不允许取消，订单ID: {}, 当前状态: {}", orderId, order.getStatus());
            return false;
        }

        // 更新订单状态
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        orderStorage.put(orderId, order);

        log.info("订单取消成功，订单ID: {}, 用户ID: {}, 商品: {}", orderId, order.getUserId(), order.getProductName());
        return true;
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 是否支付成功
     */
    public boolean payOrder(String orderId) {
        Order order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("订单不存在，订单ID: {}", orderId);
            return false;
        }

        // 只有待支付状态的订单才能支付
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("订单状态不允许支付，订单ID: {}, 当前状态: {}", orderId, order.getStatus());
            return false;
        }

        // 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        orderStorage.put(orderId, order);

        log.info("订单支付成功，订单ID: {}, 用户ID: {}, 商品: {}", orderId, order.getUserId(), order.getProductName());
        return true;
    }

    /**
     * 根据订单ID获取订单
     *
     * @param orderId 订单ID
     * @return 订单对象
     */
    public Order getOrder(String orderId) {
        return orderStorage.get(orderId);
    }
}


