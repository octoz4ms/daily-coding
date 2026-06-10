package com.example.order.controller;

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
    public Result<OrderDetailResponse> getOrderDetail(@PathVariable("id") Long id) {
        log.info("查询订单详情, orderId={}", id);

        OrderDetailResponse detail = orderService.getOrderDetail(id);
        return Result.success(detail);
    }

    @PostMapping
    public Result<OrderDetailResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("创建订单, userId={}, product={}, amount={}",
                request.getUserId(), request.getProductName(), request.getAmount());
        OrderDetailResponse detail = orderService.createOrder(request);
        return Result.success("订单创建成功", detail);
    }
}
