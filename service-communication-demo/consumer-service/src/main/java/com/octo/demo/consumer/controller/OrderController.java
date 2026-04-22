package com.octo.demo.consumer.controller;

import com.octo.demo.common.dto.OrderDTO;
import com.octo.demo.common.dto.Result;
import com.octo.demo.consumer.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 * <p>
 * 演示三种服务间调用方式，以及本地部署和跨服务器部署的差异。
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/feign/{id}")
    public Result<OrderDTO> getOrderWithFeign(@PathVariable Long id) {
        log.info("【OpenFeign方式】获取订单，ID: {}", id);
        OrderDTO order = orderService.getOrderWithFeign(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/rest/{id}")
    public Result<OrderDTO> getOrderWithRestTemplate(@PathVariable Long id) {
        log.info("【RestTemplate方式】获取订单，ID: {}", id);
        OrderDTO order = orderService.getOrderWithRestTemplate(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/webclient/{id}")
    public Result<OrderDTO> getOrderWithWebClient(@PathVariable Long id) {
        log.info("【WebClient方式】获取订单，ID: {}", id);
        OrderDTO order = orderService.getOrderWithWebClient(id);
        if (order == null) {
            return Result.fail(404, "订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/communication-methods")
    public Result<Map<String, Object>> compareCommunicationMethods() {
        log.info("查看服务间调用方式对比");
        return Result.success(orderService.compareCommunicationMethods());
    }

    @GetMapping("/deployment-differences")
    public Result<Map<String, Object>> compareDeploymentScenarios() {
        log.info("查看本地部署与跨服务器部署差异");
        return Result.success(orderService.compareDeploymentScenarios());
    }

    @GetMapping
    public Result<List<OrderDTO>> listOrders() {
        log.info("获取订单列表");
        return Result.success(orderService.listOrders());
    }

    @PostMapping
    public Result<OrderDTO> createOrder(@RequestBody OrderDTO order) {
        log.info("创建订单: {}", order);
        return Result.success("创建成功", orderService.createOrder(order));
    }
}
