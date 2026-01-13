package com.octo.eum.security;

import com.octo.eum.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Token安全增强器 - 防止重放攻击、频率限制等
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenSecurityEnhancer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final TokenFingerprintUtils fingerprintUtils;

    // Redis Key前缀
    private static final String REPLAY_ATTACK_PREFIX = "security:replay:";
    private static final String RATE_LIMIT_PREFIX = "security:ratelimit:";
    private static final String SUSPICIOUS_ACTIVITY_PREFIX = "security:suspicious:";

    /**
     * 检查重放攻击
     * 基于Token + 时间戳 + 请求内容哈希
     */
    public boolean checkReplayAttack(String token, HttpServletRequest request, String requestBody) {
        try {
            // 生成请求指纹
            String requestFingerprint = generateRequestFingerprint(token, request, requestBody);

            // 检查是否已处理过
            String key = REPLAY_ATTACK_PREFIX + requestFingerprint;
            Boolean exists = redisTemplate.hasKey(key);

            if (Boolean.TRUE.equals(exists)) {
                log.warn("检测到重放攻击: {}", requestFingerprint);
                recordSuspiciousActivity(request, "REPLAY_ATTACK");
                return false;
            }

            // 标记为已处理，5分钟内不允许重复
            redisTemplate.opsForValue().set(key, "1", 5, TimeUnit.MINUTES);
            return true;

        } catch (Exception e) {
            log.error("检查重放攻击失败", e);
            return true; // 出错时允许通过，避免误拦截
        }
    }

    /**
     * 频率限制检查
     */
    public boolean checkRateLimit(String identifier, int maxRequests, Duration window) {
        try {
            String key = RATE_LIMIT_PREFIX + identifier;

            // 使用Redis的原子操作
            Long currentCount = redisTemplate.opsForValue().increment(key);

            // 第一次请求时设置过期时间
            if (currentCount == 1) {
                redisTemplate.expire(key, window);
            }

            if (currentCount > maxRequests) {
                log.warn("频率限制触发: {} - {} requests in window", identifier, currentCount);
                recordSuspiciousActivity(null, "RATE_LIMIT_EXCEEDED");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("频率限制检查失败", e);
            return true;
        }
    }

    /**
     * 检查异常登录模式
     */
    public boolean checkSuspiciousLogin(HttpServletRequest request, String username) {
        String ip = IpUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 检查IP是否在黑名单中
        if (isIpBlacklisted(ip)) {
            log.warn("黑名单IP尝试登录: {} - {}", ip, username);
            return false;
        }

        // 检查是否为可疑用户代理
        if (isSuspiciousUserAgent(userAgent)) {
            log.warn("可疑User-Agent: {} - {}", userAgent, username);
            recordSuspiciousActivity(request, "SUSPICIOUS_USER_AGENT");
        }

        // 检查非常规登录时间
        if (isUnusualLoginTime()) {
            log.warn("非常规登录时间: {}", username);
            recordSuspiciousActivity(request, "UNUSUAL_LOGIN_TIME");
        }

        // 检查地理位置异常（需要集成IP地理位置库）
        // 这里可以添加地理位置检查逻辑

        return true;
    }

    /**
     * 检查Token泄露风险
     */
    public boolean checkTokenLeakageRisk(String token, HttpServletRequest request) {
        try {
            String ip = IpUtils.getClientIp(request);

            // 检查Token是否从多个IP使用
            String tokenIpKey = "token:ips:" + token.hashCode();
            String knownIps = (String) redisTemplate.opsForValue().get(tokenIpKey);

            if (knownIps == null) {
                // 第一次使用
                redisTemplate.opsForValue().set(tokenIpKey, ip, 24, TimeUnit.HOURS);
            } else if (!knownIps.contains(ip)) {
                // 新IP使用
                String updatedIps = knownIps + "," + ip;
                redisTemplate.opsForValue().set(tokenIpKey, updatedIps, 24, TimeUnit.HOURS);

                String[] ipArray = updatedIps.split(",");
                if (ipArray.length > 3) {
                    log.warn("Token从多个IP使用，疑似泄露: {} - IPs: {}", token.substring(0, 10) + "...", updatedIps);
                    recordSuspiciousActivity(request, "TOKEN_LEAKAGE_RISK");
                    // 可以选择自动失效Token
                    // return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("检查Token泄露风险失败", e);
            return true;
        }
    }

    /**
     * 生成请求指纹
     */
    private String generateRequestFingerprint(String token, HttpServletRequest request, String requestBody) {
        String timestamp = request.getHeader("X-Timestamp");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String ip = IpUtils.getClientIp(request);

        String content = token + method + uri + ip + (timestamp != null ? timestamp : "") +
                        (requestBody != null ? requestBody : "");

        return String.valueOf(content.hashCode());
    }

    /**
     * 检查IP是否在黑名单中
     */
    private boolean isIpBlacklisted(String ip) {
        String key = "blacklist:ip:" + ip;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查可疑User-Agent
     */
    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null) return true;

        // 检查是否为空或太短
        if (userAgent.trim().length() < 10) return true;

        // 检查是否包含自动化工具特征
        String lowerAgent = userAgent.toLowerCase();
        return lowerAgent.contains("curl") ||
               lowerAgent.contains("wget") ||
               lowerAgent.contains("python") ||
               lowerAgent.contains("bot") ||
               lowerAgent.contains("spider");
    }

    /**
     * 检查非常规登录时间
     */
    private boolean isUnusualLoginTime() {
        // 这里可以根据业务需求定义非常规时间
        // 例如：凌晨2-5点
        int hour = java.time.LocalDateTime.now().getHour();
        return hour >= 2 && hour <= 5;
    }

    /**
     * 记录可疑活动
     */
    private void recordSuspiciousActivity(HttpServletRequest request, String activityType) {
        try {
            String ip = request != null ? IpUtils.getClientIp(request) : "unknown";
            String key = SUSPICIOUS_ACTIVITY_PREFIX + ip + ":" + activityType;

            // 记录活动次数
            Long count = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);

            // 如果活动过于频繁，加入临时黑名单
            if (count > 10) {
                String blacklistKey = "blacklist:ip:" + ip;
                redisTemplate.opsForValue().set(blacklistKey, activityType, 1, TimeUnit.HOURS);
                log.warn("IP {} 因可疑活动过多加入临时黑名单", ip);
            }

        } catch (Exception e) {
            log.error("记录可疑活动失败", e);
        }
    }

    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        // 这个方法可以定期调用来清理过期的安全数据
        log.debug("清理过期安全数据");
        // Redis的过期机制会自动清理，这里主要是日志记录
    }
}
