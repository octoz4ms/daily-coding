package com.octo.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置 - 暂时禁用
 */
@Slf4j
@Configuration
public class AlipayConfig {

    // TODO: 支付宝SDK依赖暂时注释，需要时取消注释并实现
    // @Value("${alipay.app-id}")
    // private String appId;
    //
    // @Value("${alipay.private-key}")
    // private String privateKey;
    //
    // @Value("${alipay.public-key}")
    // private String publicKey;
    //
    // @Value("${alipay.gateway-url}")
    // private String gatewayUrl;
    //
    // @Bean
    // public AlipayClient alipayClient() {
    //     return new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
    // }
}
