package com.octo.rc.rabbitmq.controller;

import com.octo.rc.rabbitmq.entity.Order;
import com.octo.rc.rabbitmq.service.OrderDLXService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单控制器 - 基于 TTL + 死信队列方式
 * 
 * 测试流程：
 * 1. 创建订单：POST /order-dlx/create?userId=1&productId=100&productName=iPhone&amount=6999
 * 2. 查询订单：GET /order-dlx/query?orderId=xxx
 * 3. 等待超时后，订单会自动取消（默认30分钟，可修改配置）
 * 4. 或者手动支付：POST /order-dlx/pay?orderId=xxx
 */
@RestController
@RequestMapping("/order-dlx")
@Slf4j
public class OrderDLXController {

    @Autowired
    private OrderDLXService orderDLXService;

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
        return orderDLXService.createOrder(userId, productId, productName, amount);
    }

    /**
     * 支付订单
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @PostMapping("/pay")
    public String payOrder(@RequestParam String orderId) {
        boolean success = orderDLXService.payOrder(orderId);
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
        boolean success = orderDLXService.cancelOrder(orderId);
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
        return orderDLXService.getOrder(orderId);
    }
}
