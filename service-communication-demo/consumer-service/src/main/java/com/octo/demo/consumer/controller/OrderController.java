package com.octo.demo.consumer.controller;

import com.octo.demo.common.dto.OrderDTO;
import com.octo.demo.common.dto.Result;
import com.octo.demo.consumer.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * <p>
 * 演示两种服务间调用方式
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 获取订单（使用 OpenFeign 调用用户服务）
     * <p>
     * 推荐方式：声明式、简洁、支持降级
     */
    @GetMapping("/feign/{id}")
    public Result<OrderDTO> getOrderWithFeign(@PathVariable Long id) {
        log.info("【OpenFeign方式】获取订单，ID: {}", id);
        OrderDTO order = orderService.getOrderWithFeign(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 获取订单（使用 RestTemplate 调用用户服务）
     * <p>
     * 传统方式：灵活但代码冗长
     */
    @GetMapping("/rest/{id}")
    public Result<OrderDTO> getOrderWithRestTemplate(@PathVariable Long id) {
        log.info("【RestTemplate方式】获取订单，ID: {}", id);
        OrderDTO order = orderService.getOrderWithRestTemplate(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 获取订单列表
     */
    @GetMapping
    public Result<List<OrderDTO>> listOrders() {
        log.info("获取订单列表");
        return Result.success(orderService.listOrders());
    }

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderDTO> createOrder(@RequestBody OrderDTO order) {
        log.info("创建订单: {}", order);
        return Result.success("创建成功", orderService.createOrder(order));
    }
}

