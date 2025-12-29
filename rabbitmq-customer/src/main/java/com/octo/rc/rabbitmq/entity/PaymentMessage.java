package com.octo.rc.rabbitmq.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付单号
     */
    private String paymentId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式：ALIPAY, WECHAT, BANK_CARD
     */
    private String paymentMethod;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 重试次数
     */
    private int retryCount;
}




