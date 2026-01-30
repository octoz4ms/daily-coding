package com.example.payment.controller;

import com.example.payment.dto.request.PayOrderRequest;
import com.example.payment.dto.response.ApiResponse;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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
     * 创建支付单（发起支付）
     */
    @PostMapping("/create")
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PayOrderRequest request) {
        log.info("创建支付单请求，orderNo: {}, paymentMethod: {}", request.getOrderNo(), request.getPaymentMethod());
        PaymentResponse response = paymentService.createPayment(request);
        return ApiResponse.success("支付单创建成功", response);
    }

    /**
     * 查询支付单状态
     */
    @GetMapping("/query/{paymentNo}")
    public ApiResponse<PaymentResponse> queryPayment(@PathVariable @NotBlank String paymentNo) {
        log.info("查询支付单，paymentNo: {}", paymentNo);
        PaymentResponse response = paymentService.queryPayment(paymentNo);
        if (response == null) {
            return ApiResponse.error(404, "支付单不存在");
        }
        return ApiResponse.success(response);
    }

    /**
     * 根据订单号查询支付单
     */
    @GetMapping("/query/order/{orderNo}")
    public ApiResponse<PaymentResponse> queryPaymentByOrderNo(@PathVariable @NotBlank String orderNo) {
        log.info("根据订单号查询支付单，orderNo: {}", orderNo);
        PaymentResponse response = paymentService.queryPaymentByOrderNo(orderNo);
        if (response == null) {
            return ApiResponse.error(404, "支付单不存在");
        }
        return ApiResponse.success(response);
    }

    /**
     * 关闭支付单
     */
    @PostMapping("/close/{paymentNo}")
    public ApiResponse<Void> closePayment(@PathVariable @NotBlank String paymentNo) {
        log.info("关闭支付单请求，paymentNo: {}", paymentNo);
        boolean success = paymentService.closePayment(paymentNo);
        if (success) {
            return ApiResponse.success("支付单关闭成功", null);
        }
        return ApiResponse.error("支付单关闭失败");
    }

    /**
     * 主动查询第三方支付状态
     */
    @PostMapping("/sync/{paymentNo}")
    public ApiResponse<PaymentResponse> syncPaymentStatus(@PathVariable @NotBlank String paymentNo) {
        log.info("同步支付状态，paymentNo: {}", paymentNo);
        PaymentResponse response = paymentService.syncPaymentStatus(paymentNo);
        return ApiResponse.success(response);
    }

    /**
     * 微信支付回调
     */
    @PostMapping("/callback/wechat")
    public String wechatCallback(HttpServletRequest request, @RequestBody(required = false) String body) {
        log.info("收到微信支付回调");

        try {
            // 解析回调参数（实际项目中需要解析XML）
            Map<String, String> params = parseWechatCallback(body);
            
            // TODO: 验证微信签名
            
            boolean success = paymentService.handleWechatCallback(params);

            if (success) {
                return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            }
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
        }

        return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
    }

    /**
     * 支付宝回调
     */
    @PostMapping("/callback/alipay")
    public String alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调");

        try {
            // 解析回调参数
            Map<String, String> params = parseAlipayCallback(request);
            
            // TODO: 验证支付宝签名
            
            boolean success = paymentService.handleAlipayCallback(params);

            if (success) {
                return "success";
            }
        } catch (Exception e) {
            log.error("支付宝回调处理失败", e);
        }

        return "fail";
    }

    /**
     * 解析微信回调数据（简化版，实际需要解析XML）
     */
    private Map<String, String> parseWechatCallback(String xml) {
        Map<String, String> params = new HashMap<>();
        // TODO: 实际项目中需要解析XML
        // 这里模拟数据
        if (xml != null && xml.contains("SUCCESS")) {
            params.put("result_code", "SUCCESS");
        }
        return params;
    }

    /**
     * 解析支付宝回调数据
     */
    private Map<String, String> parseAlipayCallback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        return params;
    }
}

