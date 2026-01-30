package com.octo.payment.controller;

import com.octo.payment.dto.PaymentRequest;
import com.octo.payment.dto.PaymentResponse;
import com.octo.payment.dto.RefundRequest;
import com.octo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@Validated
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 创建支付订单
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("创建支付订单请求：{}", request);
        PaymentResponse response = paymentService.createPayment(request);

        if (PaymentResponse.builder().build().getStatus() != null &&
            "FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 查询支付订单
     */
    @GetMapping("/query/{orderId}")
    public ResponseEntity<PaymentResponse> queryPayment(@PathVariable String orderId) {
        log.info("查询支付订单：{}", orderId);
        PaymentResponse response = paymentService.queryPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 关闭支付订单
     */
    @PostMapping("/close/{orderId}")
    public ResponseEntity<?> closePayment(@PathVariable String orderId) {
        log.info("关闭支付订单：{}", orderId);
        boolean success = paymentService.closePayment(orderId);

        if (success) {
            return ResponseEntity.ok().body("{\"success\":true,\"message\":\"订单关闭成功\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"success\":false,\"message\":\"订单关闭失败\"}");
        }
    }

    /**
     * 申请退款
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refund(@Valid @RequestBody RefundRequest request) {
        log.info("申请退款：{}", request);
        boolean success = paymentService.refund(request);

        if (success) {
            return ResponseEntity.ok().body("{\"success\":true,\"message\":\"退款申请成功\"}");
        } else {
            return ResponseEntity.badRequest().body("{\"success\":false,\"message\":\"退款申请失败\"}");
        }
    }

    /**
     * 查询退款状态
     */
    @GetMapping("/refund/{refundOrderId}")
    public ResponseEntity<PaymentResponse> queryRefund(@PathVariable String refundOrderId) {
        log.info("查询退款状态：{}", refundOrderId);
        PaymentResponse response = paymentService.queryRefund(refundOrderId);
        return ResponseEntity.ok(response);
    }
}
