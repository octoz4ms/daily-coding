package com.octo.payment.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 商户订单号
     */
    private String merchantOrderId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付状态
     */
    private String status;

    /**
     * 支付参数（用于前端调用支付）
     */
    private Object payParams;

    /**
     * 二维码URL（微信支付使用）
     */
    private String qrCodeUrl;

    /**
     * 支付跳转URL（支付宝使用）
     */
    private String payUrl;

    /**
     * 错误信息
     */
    private String errorMessage;
}
