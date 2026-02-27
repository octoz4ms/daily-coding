package com.octo.wechatpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Native 支付下单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NativePrepayResponse {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 二维码链接，用于生成支付二维码
     */
    private String codeUrl;

    /**
     * 预支付交易会话标识
     */
    private String prepayId;
}
