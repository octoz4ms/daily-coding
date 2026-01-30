package com.octo.payment.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallback {

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 商户订单号
     */
    private String merchantOrderId;

    /**
     * 支付平台交易号
     */
    private String transactionId;

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
     * 支付完成时间
     */
    private LocalDateTime paidTime;

    /**
     * 回调原始数据
     */
    private String rawData;

    /**
     * 是否处理成功
     */
    private boolean processed;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;
}
