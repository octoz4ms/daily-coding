package com.octo.wechatpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQueryResponse {

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 微信支付订单号
     */
    private String transactionId;

    /**
     * 交易状态：SUCCESS-支付成功, REFUND-转入退款, NOTPAY-未支付, CLOSED-已关闭, REVOKED-已撤销, USERPAYING-用户支付中, PAYERROR-支付失败
     */
    private String tradeState;

    /**
     * 交易状态描述
     */
    private String tradeStateDesc;

    /**
     * 支付完成时间（ISO 8601 格式）
     */
    private String successTime;
}
