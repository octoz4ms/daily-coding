package com.example.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.entity.Order;
import com.example.common.result.Result;
import com.example.order.dto.OrderDetailResponse;
import com.example.order.service.OrderService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable(name = "id") Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @GetMapping("/with-user/{id}")
    @SentinelResource(value = "getOrderWithUser", blockHandler = "getOrderWithUserBlockHandler")
    public Result<OrderDetailResponse> getOrderWithUser(@PathVariable(name = "id") Long id) {
        return Result.success(orderService.getOrderWithUserStrict(id));
    }

    @GetMapping("/with-user-tolerant/{id}")
    public Result<OrderDetailResponse> getOrderWithUserTolerant(@PathVariable(name = "id") Long id) {
        return Result.success(orderService.getOrderWithUserTolerant(id));
    }

    public Result<OrderDetailResponse> getOrderWithUserBlockHandler(Long id, Throwable ex) {
        return Result.fail(429, "当前访问过于频繁，请稍后重试");
    }

    @GetMapping("/list")
    public Result<Map<Long, Order>> listOrders() {
        return Result.success(orderService.listOrders());
    }
}
