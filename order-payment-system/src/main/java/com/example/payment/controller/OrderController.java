package com.example.payment.controller;

import com.example.payment.dto.request.CreateOrderRequest;
import com.example.payment.dto.response.ApiResponse;
import com.example.payment.dto.response.OrderResponse;
import com.example.payment.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

/**
 * 订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取提交令牌（防重复提交）
     * 在进入结算页时调用
     */
    @GetMapping("/submit-token")
    public ApiResponse<String> getSubmitToken(@RequestParam Long userId) {
        log.info("获取提交令牌，userId: {}", userId);
        String token = orderService.generateSubmitToken(userId);
        return ApiResponse.success("获取成功", token);
    }

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("创建订单请求，userId: {}, productId: {}", request.getUserId(), request.getProductId());
        OrderResponse response = orderService.createOrder(request);
        return ApiResponse.success("订单创建成功", response);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/detail/{orderNo}")
    public ApiResponse<OrderResponse> getOrderDetail(@PathVariable @NotBlank String orderNo) {
        log.info("查询订单详情，orderNo: {}", orderNo);
        OrderResponse response = orderService.getOrderByOrderNo(orderNo);
        if (response == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        return ApiResponse.success(response);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{orderNo}")
    public ApiResponse<Void> cancelOrder(
            @PathVariable @NotBlank String orderNo,
            @RequestParam(required = false, defaultValue = "用户主动取消") String reason) {
        log.info("取消订单请求，orderNo: {}, reason: {}", orderNo, reason);
        boolean success = orderService.cancelOrder(orderNo, reason);
        if (success) {
            return ApiResponse.success("订单取消成功", null);
        }
        return ApiResponse.error("订单取消失败");
    }
}

