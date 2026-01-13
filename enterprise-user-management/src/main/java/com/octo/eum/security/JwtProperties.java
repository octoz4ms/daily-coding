package com.octo.eum.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性 - 大厂级Token管理策略
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
     * 访问Token过期时间（秒），默认15分钟 - 大厂通常较短以提高安全性
     */
    private Long accessTokenExpiration = 900L;

    /**
     * 刷新Token过期时间（秒），默认30天 - 支持长期会话
     */
    private Long refreshTokenExpiration = 2592000L;

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
     * 自动刷新阈值（秒）- 剩余多少秒时自动刷新，默认5分钟
     */
    private Long autoRefreshThreshold = 300L;

    /**
     * 是否启用单设备登录，默认false
     */
    private Boolean singleDeviceLogin = false;

    /**
     * 是否启用Token指纹校验（基于IP+UserAgent），默认true
     */
    private Boolean enableFingerprint = true;

    /**
     * 是否启用滑动窗口续签，默认true
     */
    private Boolean enableSlidingRefresh = true;

    /**
     * 续签最大次数限制，默认10次
     */
    private Integer maxRefreshCount = 10;

    /**
     * Token版本号，用于强制失效旧Token
     */
    private Integer tokenVersion = 1;

    /**
     * 是否启用并发请求Token刷新保护，默认true
     */
    private Boolean enableConcurrentRefreshProtection = true;

    /**
     * 刷新Token时的锁超时时间（秒），默认30秒
     */
    private Long refreshLockTimeout = 30L;
}

