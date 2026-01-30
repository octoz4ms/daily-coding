package com.octo.payment.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置 - 暂时禁用
 */
@Slf4j
@Configuration
public class WeChatPayConfig {

    // TODO: 微信支付SDK依赖暂时注释，需要时取消注释并实现
    // @Value("${wechat.pay.app-id}")
    // private String appId;
    //
    // @Value("${wechat.pay.mch-id}")
    // private String mchId;
    //
    // @Value("${wechat.pay.private-key-path}")
    // private String privateKeyPath;
    //
    // @Value("${wechat.pay.merchant-serial-number}")
    // private String merchantSerialNumber;
    //
    // @Value("${wechat.pay.api-v3-private-key}")
    // private String apiV3PrivateKey;
    //
    // @Bean
    // public Config wechatPayConfig() throws IOException {
    //     ClassPathResource resource = new ClassPathResource(privateKeyPath);
    //     PrivateKey privateKey = com.wechat.pay.java.core.util.PemUtil.loadPrivateKey(resource.getInputStream());
    //
    //     return new RSAPublicKeyConfig.Builder()
    //             .merchantId(mchId)
    //             .privateKey(privateKey)
    //             .merchantSerialNumber(merchantSerialNumber)
    //             .apiV3PrivateKey(apiV3PrivateKey)
    //             .build();
    // }
    //
    // @Bean
    // public NativePayService nativePayService(Config config) {
    //     return new NativePayService.Builder().config(config).build();
    // }
}
