package com.octo.eum.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
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
    private String secret = "your-256-bit-secret-key-must-be-at-least-32-characters";

    /**
     * 访问Token过期时间（秒），默认2小时
     */
    private Long accessTokenExpiration = 7200L;

    /**
     * 刷新Token过期时间（秒），默认7天
     */
    private Long refreshTokenExpiration = 604800L;

    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token请求头
     */
    private String header = "Authorization";

    /**
     * 签发者
     */
    private String issuer = "enterprise-user-management";
}

