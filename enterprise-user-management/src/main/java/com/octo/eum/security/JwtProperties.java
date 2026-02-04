package com.octo.eum.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置
 *
 * @author octo
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥
     */
    private String secret = "your-256-bit-secret-key-for-jwt-authentication";

    /**
     * Access Token有效期（秒），默认30分钟
     */
    private Long accessTokenExpiration = 1800L;

    /**
     * Refresh Token有效期（秒），默认7天
     */
    private Long refreshTokenExpiration = 604800L;

    /**
     * 签发者
     */
    private String issuer = "enterprise-user-management";

    /**
     * 登录策略
     * - MULTI: 多端共存
     * - SINGLE: 单设备
     * - SAME_TYPE_KICK: 同端互踢（推荐）
     * - MAX_DEVICE: 最大设备数限制
     */
    private LoginPolicy loginPolicy = LoginPolicy.SAME_TYPE_KICK;

    /**
     * 最大设备数（仅 MAX_DEVICE 策略生效）
     * 默认5个设备
     */
    private Integer maxDeviceCount = 5;
}
