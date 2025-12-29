package com.octo.rc.rabbitmq.controller;

import com.octo.rc.rabbitmq.entity.Order;
import com.octo.rc.rabbitmq.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param amount      订单金额
     * @return 订单对象
     */
    @PostMapping("/create")
    public Order createOrder(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam String productName,
            @RequestParam BigDecimal amount) {
        return orderService.createOrder(userId, productId, productName, amount);
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/pay")
    public String payOrder(@RequestParam String orderId) {
        boolean success = orderService.payOrder(orderId);
        return success ? "支付成功" : "支付失败，订单不存在或状态不允许支付";
    }

    /**
     * 手动取消订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/cancel")
    public String cancelOrder(@RequestParam String orderId) {
        boolean success = orderService.cancelOrder(orderId);
        return success ? "取消成功" : "取消失败，订单不存在或状态不允许取消";
    }

    /**
     * 查询订单
     *
     * @param orderId 订单ID
     * @return 订单对象
     */
    @GetMapping("/query")
    public Order queryOrder(@RequestParam String orderId) {
        return orderService.getOrder(orderId);
    }
}





