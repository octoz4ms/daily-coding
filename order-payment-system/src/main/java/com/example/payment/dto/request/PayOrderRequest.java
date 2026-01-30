package com.example.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 支付订单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderRequest {

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 支付方式：1-微信 2-支付宝 3-银行卡 4-余额
     */
    @NotNull(message = "支付方式不能为空")
    private Integer paymentMethod;

    /**
     * 支付成功回调地址
     */
    private String returnUrl;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 支付场景：APP/H5/NATIVE/JSAPI
     */
    private String tradeType;
}

