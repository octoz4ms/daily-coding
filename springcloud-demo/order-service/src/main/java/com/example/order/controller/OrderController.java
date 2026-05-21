package com.example.order.controller;

import com.example.common.entity.Order;
import com.example.common.entity.User;
import com.example.common.result.Result;
import com.example.order.feign.UserFeignClient;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private UserFeignClient userFeignClient;

    // 模拟数据库
    private static final Map<Long, Order> ORDER_MAP = new HashMap<>();

    static {
        ORDER_MAP.put(1L, new Order(1L, "ORD001", 1L, "iPhone 15 Pro", 8999.0));
        ORDER_MAP.put(2L, new Order(2L, "ORD002", 2L, "MacBook Pro", 14999.0));
        ORDER_MAP.put(3L, new Order(3L, "ORD003", 1L, "AirPods Pro", 1899.0));
    }

    /**
     * 根据ID获取订单
     */
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable(name = "id") Long id) {
        Order order = ORDER_MAP.get(id);
        if (order != null) {
            return Result.success(order);
        }
        return Result.fail("订单不存在");
    }

    /**
     * 获取订单及用户信息（演示服务间调用）
     */
    @GetMapping("/with-user/{id}")
    public Result<Map<String, Object>> getOrderWithUser(@PathVariable(name = "id") Long id) {
        Order order = ORDER_MAP.get(id);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 通过 Feign 远程调用用户服务获取用户信息
        Result<User> userResult = userFeignClient.getUserById(order.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("user", userResult.getData());

        return Result.success(result);
    }

    /**
     * 获取所有订单
     */
    @GetMapping("/list")
    public Result<Map<Long, Order>> listOrders() {
        return Result.success(ORDER_MAP);
    }
}
