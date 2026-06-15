package com.example.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.common.result.Result;
import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderDetailResponse;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    @SentinelResource(value = "order:getOrderDetail", blockHandler = "handleGetOrderDetailBlock", fallback = "handleGetOrderDetailFallback")
    public Result<OrderDetailResponse> getOrderDetail(@PathVariable("id") Long id) {
        log.info("查询订单详情, orderId={}", id);

        OrderDetailResponse detail = orderService.getOrderDetail(id);
        return Result.success(detail);
    }


    @GetMapping("/check")
    @SentinelResource(value = "order:check", blockHandler = "handleGetOrderDetailBlock", fallback = "handleGetOrderDetailFallback")
    public Result<Object> getOrderCheck(@PathVariable("id") Long id) {
        log.info("查询订单详情, orderId={}", id);
        return Result.success(null);
    }

    @PostMapping
    @SentinelResource(value = "order:createOrder", blockHandler = "handleCreateOrderBlock", fallback = "handleCreateOrderFallback")
    public Result<OrderDetailResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("创建订单, userId={}, product={}, amount={}",
                request.getUserId(), request.getProductName(), request.getAmount());
        OrderDetailResponse detail = orderService.createOrder(request);
        return Result.success("订单创建成功", detail);
    }

    public Result<OrderDetailResponse> handleGetOrderDetailBlock(Long id, BlockException ex) {
        log.warn("[Sentinel] 查询订单详情被限流/熔断, orderId={}, rule={}", id, ex.getRule());
        return Result.fail(429, "当前查询人数较多，请稍后重试");
    }

    public Result<OrderDetailResponse> handleGetOrderDetailFallback(Long id, Throwable ex) {
        log.error("[Sentinel] 查询订单详情降级处理, orderId={}", id, ex);
        return Result.fail(500, "订单详情服务暂时不可用，请稍后重试");
    }

    public Result<OrderDetailResponse> handleCreateOrderBlock(OrderCreateRequest request, BlockException ex) {
        log.warn("[Sentinel] 创建订单被限流/熔断, userId={}, rule={}", request.getUserId(), ex.getRule());
        return Result.fail(429, "当前下单人数过多，请稍后重试");
    }

    public Result<OrderDetailResponse> handleCreateOrderFallback(OrderCreateRequest request, Throwable ex) {
        log.error("[Sentinel] 创建订单降级处理, userId={}", request.getUserId(), ex);
        return Result.fail(500, "下单服务暂时不可用，请稍后重试");
    }
}
