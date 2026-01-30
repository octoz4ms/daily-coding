package com.example.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    /**
     * 待支付
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 已支付
     */
    PAID(1, "已支付"),

    /**
     * 已发货
     */
    SHIPPED(2, "已发货"),

    /**
     * 已签收
     */
    RECEIVED(3, "已签收"),

    /**
     * 已完成
     */
    COMPLETED(4, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(5, "已取消"),

    /**
     * 已关闭（超时关闭）
     */
    CLOSED(6, "已关闭"),

    /**
     * 退款中
     */
    REFUNDING(7, "退款中"),

    /**
     * 已退款
     */
    REFUNDED(8, "已退款");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否可以支付
     */
    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING_PAYMENT;
    }

    /**
     * 判断是否可以退款
     */
    public boolean canRefund() {
        return this == PAID || this == SHIPPED || this == RECEIVED;
    }
}

