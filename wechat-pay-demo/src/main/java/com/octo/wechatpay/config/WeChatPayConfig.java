package com.octo.wechatpay.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.util.IOUtil;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

/**
 * 微信支付配置 - 使用 RSAAutoCertificateConfig 自动更新平台证书
 */
@Slf4j
@Configuration
public class WeChatPayConfig {

    @Bean
    public Config wechatPayConfig(WeChatPayProperties properties) throws IOException {
        String privateKeyPath = properties.getPrivateKeyPath();
        PrivateKey privateKey;

        // 支持 classpath: 和绝对路径
        if (privateKeyPath.startsWith("classpath:")) {
            String path = privateKeyPath.substring("classpath:".length());
            Resource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new IllegalStateException("商户私钥文件不存在: " + privateKeyPath);
            }
            try (InputStream is = resource.getInputStream()) {
                String keyString = IOUtil.toString(is);
                privateKey = PemUtil.loadPrivateKeyFromString(keyString);
            }
        } else {
            privateKey = PemUtil.loadPrivateKeyFromPath(privateKeyPath);
        }

        Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(properties.getMchId())
                .privateKey(privateKey)
                .merchantSerialNumber(properties.getMerchantSerialNumber())
                .apiV3Key(properties.getApiV3Key())
                .build();

        log.info("微信支付配置初始化完成，商户号: {}", properties.getMchId());
        return config;
    }

    @Bean
    public NativePayService nativePayService(Config config) {
        return new NativePayService.Builder().config(config).build();
    }
}
