package com.example.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.payment.enums.PaymentMethod;
import com.example.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_payment_order")
public class PaymentOrder {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付单号（唯一）
     */
    private String paymentNo;

    /**
     * 关联订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式：1-微信 2-支付宝 3-银行卡 4-余额
     */
    private Integer paymentMethod;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 第三方支付交易号（微信/支付宝返回）
     */
    private String transactionId;

    /**
     * 支付描述/商品描述
     */
    private String description;

    /**
     * 支付回调通知地址
     */
    private String notifyUrl;

    /**
     * 支付成功跳转地址
     */
    private String returnUrl;

    /**
     * 支付二维码/链接
     */
    private String payUrl;

    /**
     * 预支付ID（微信）
     */
    private String prepayId;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 回调原始数据
     */
    private String callbackData;

    /**
     * 回调时间
     */
    private LocalDateTime callbackTime;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 附加数据（业务透传）
     */
    private String attach;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;

    /**
     * 获取支付状态枚举
     */
    public PaymentStatus getPaymentStatus() {
        return PaymentStatus.fromCode(this.status);
    }

    /**
     * 设置支付状态
     */
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.status = paymentStatus.getCode();
    }

    /**
     * 获取支付方式枚举
     */
    public PaymentMethod getPaymentMethodEnum() {
        return PaymentMethod.fromCode(this.paymentMethod);
    }

    /**
     * 设置支付方式
     */
    public void setPaymentMethodEnum(PaymentMethod method) {
        this.paymentMethod = method.getCode();
    }

    /**
     * 判断是否已过期
     */
    public boolean isExpired() {
        return this.expireTime != null && LocalDateTime.now().isAfter(this.expireTime);
    }
}

