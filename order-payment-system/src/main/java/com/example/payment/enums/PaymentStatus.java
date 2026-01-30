package com.example.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {

    /**
     * 待支付
     */
    PENDING(0, "待支付"),

    /**
     * 支付中（已唤起支付）
     */
    PAYING(1, "支付中"),

    /**
     * 支付成功
     */
    SUCCESS(2, "支付成功"),

    /**
     * 支付失败
     */
    FAILED(3, "支付失败"),

    /**
     * 已关闭
     */
    CLOSED(4, "已关闭"),

    /**
     * 已退款
     */
    REFUNDED(5, "已退款"),

    /**
     * 部分退款
     */
    PARTIAL_REFUNDED(6, "部分退款");

    private final Integer code;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为终态
     */
    public boolean isFinalStatus() {
        return this == SUCCESS || this == FAILED || this == CLOSED || this == REFUNDED;
    }
}

