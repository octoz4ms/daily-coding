package com.octo.payment.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    /**
     * 订单号
     */
    private String orderId;

    /**
     * 商户订单号
     */
    private String merchantOrderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private com.octo.payment.enums.PaymentMethod paymentMethod;

    /**
     * 支付状态
     */
    private com.octo.payment.enums.PaymentStatus status;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 支付完成时间
     */
    private LocalDateTime paidTime;

    /**
     * 订单创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付平台返回的交易号
     */
    private String transactionId;

    /**
     * 回调通知URL
     */
    private String notifyUrl;

    /**
     * 支付成功跳转URL
     */
    private String returnUrl;

    /**
     * 附加数据
     */
    private String attach;
}
