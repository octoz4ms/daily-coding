package com.octo.wechatpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSAPI 支付下单响应 - 用于 wx.chooseWXPay
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsapiPrepayResponse {

    /**
     * 时间戳
     */
    private String timeStamp;

    /**
     * 随机串
     */
    private String nonceStr;

    /**
     * 预支付 ID 包，格式 prepay_id=xxx（前端 wx.chooseWXPay 需要 package 字段）
     */
    @JsonProperty("package")
    private String packageVal;

    /**
     * 签名类型，RSA
     */
    private String signType;

    /**
     * 签名
     */
    private String paySign;
}
