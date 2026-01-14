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
     * 密钥 - 生产环境建议使用环境变量或密钥管理服务
     */
    private String secret = "your-256-bit-secret-key-must-be-at-least-32-characters-long-for-production";

    /**
     * 访问Token过期时间（秒），默认15分钟
     */
    private Long accessTokenExpiration = 900L;

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

    /**
     * 登录策略
     * - MULTI: 多端同时在线
     * - SINGLE: 单设备登录
     * - SAME_TYPE_KICK: 同类型设备互踢（默认）
     */
    private LoginPolicy loginPolicy = LoginPolicy.SAME_TYPE_KICK;

    /**
     * Token版本号，用于强制失效旧Token（修改密码时递增）
     */
    private Integer tokenVersion = 1;
}
