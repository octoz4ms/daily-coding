package com.octo.wechatpay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.pay")
public class WeChatPayProperties {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户 API 私钥路径
     */
    private String privateKeyPath;

    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;

    /**
     * APIv3 密钥
     */
    private String apiV3Key;

    /**
     * 支付结果通知 URL
     */
    private String notifyUrl;

    /**
     * 公众号 AppSecret（JSAPI 的 wx.config 签名需要，用于获取 jsapi_ticket）
     */
    private String appSecret;
}
