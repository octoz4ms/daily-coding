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
 * 订单服务类 - 使用 TTL + 死信队列实现订单超时取消
 * 
 * 工作流程：
 * 1. 创建订单时，发送消息到延迟队列（order.delay.queue）
 * 2. 延迟队列配置了 TTL 和死信交换机
 * 3. 消息在延迟队列中等待 TTL 时间后过期
 * 4. 过期的消息被转发到死信交换机（order.dlx.exchange）
 * 5. 死信交换机将消息路由到处理队列（order.process.queue）
 * 6. 消费者监听处理队列，检查订单状态并执行取消逻辑
 * 
 * 优势：
 * - 不需要安装额外的 RabbitMQ 插件
 * - 使用原生的 RabbitMQ 特性
 */
@Service
@Slf4j
public class OrderDLXService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 模拟订单存储（实际项目中应该使用数据库）
     */
    private final Map<String, Order> orderStorage = new ConcurrentHashMap<>();

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
        log.info("【DLX方式】创建订单成功，订单ID: {}, 用户ID: {}, 商品: {}, 金额: {}", 
                orderId, userId, productName, amount);

        // 发送消息到延迟队列，用于订单超时自动取消
        sendToDelayQueue(orderId);

        return order;
    }

    /**
     * 发送消息到延迟队列
     * 消息将在队列中等待TTL时间后转发到死信队列
     *
     * @param orderId 订单ID
     */
    private void sendToDelayQueue(String orderId) {
        rabbitTemplate.convertAndSend(
                "order.delay.exchange",
                "order.delay.routing.key",
                orderId
        );
        log.info("【DLX方式】发送订单到延迟队列，订单ID: {}", orderId);
    }

    /**
     * 处理超时订单 - 由监听器调用
     *
     * @param orderId 订单ID
     * @return 是否取消成功
     */
    public boolean handleTimeoutOrder(String orderId) {
        Order order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("【DLX方式】订单不存在，订单ID: {}", orderId);
            return false;
        }

        // 只有待支付状态的订单才需要取消
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.info("【DLX方式】订单状态已变更，无需取消，订单ID: {}, 当前状态: {}", 
                    orderId, order.getStatus());
            return false;
        }

        // 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("订单超时未支付，系统自动取消");
        orderStorage.put(orderId, order);

        log.info("【DLX方式】订单超时自动取消成功，订单ID: {}, 用户ID: {}, 商品: {}", 
                orderId, order.getUserId(), order.getProductName());
        
        // 这里可以添加其他业务逻辑，如：
        // - 释放库存
        // - 释放优惠券
        // - 发送取消通知

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
            log.warn("【DLX方式】订单不存在，订单ID: {}", orderId);
            return false;
        }

        // 只有待支付状态的订单才能支付
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("【DLX方式】订单状态不允许支付，订单ID: {}, 当前状态: {}", 
                    orderId, order.getStatus());
            return false;
        }

        // 更新订单状态
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        orderStorage.put(orderId, order);

        log.info("【DLX方式】订单支付成功，订单ID: {}, 用户ID: {}, 商品: {}", 
                orderId, order.getUserId(), order.getProductName());
        return true;
    }

    /**
     * 手动取消订单
     *
     * @param orderId 订单ID
     * @return 是否取消成功
     */
    public boolean cancelOrder(String orderId) {
        Order order = orderStorage.get(orderId);
        if (order == null) {
            log.warn("【DLX方式】订单不存在，订单ID: {}", orderId);
            return false;
        }

        // 只有待支付状态的订单才能取消
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("【DLX方式】订单状态不允许取消，订单ID: {}, 当前状态: {}", 
                    orderId, order.getStatus());
            return false;
        }

        // 更新订单状态
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason("用户手动取消");
        orderStorage.put(orderId, order);

        log.info("【DLX方式】订单手动取消成功，订单ID: {}, 用户ID: {}, 商品: {}", 
                orderId, order.getUserId(), order.getProductName());
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
