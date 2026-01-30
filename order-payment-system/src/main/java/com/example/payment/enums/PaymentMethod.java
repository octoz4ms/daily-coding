package com.example.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentMethod {

    /**
     * 微信支付
     */
    WECHAT(1, "wechat", "微信支付"),

    /**
     * 支付宝
     */
    ALIPAY(2, "alipay", "支付宝"),

    /**
     * 银行卡
     */
    BANK_CARD(3, "bank_card", "银行卡"),

    /**
     * 余额支付
     */
    BALANCE(4, "balance", "余额支付");

    private final Integer code;
    private final String name;
    private final String desc;

    /**
     * 根据code获取枚举
     */
    public static PaymentMethod fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 根据name获取枚举
     */
    public static PaymentMethod fromName(String name) {
        if (name == null) {
            return null;
        }
        for (PaymentMethod method : values()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }
}

