package com.octo.eum.service;

import cn.hutool.core.util.IdUtil;
import com.octo.eum.security.ClientType;
import com.octo.eum.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 登录Token服务
 * 
 * Redis Key:
 * - login:{userId}:{clientType}  → tokenId      (登录态)
 * - refresh:{rtid}               → uid|ct|tid   (RefreshToken，一次性)
 * - token:ver:{userId}           → version      (Token版本，全量失效)
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginTokenService {

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;

    private static final String LOGIN_KEY = "login:%s:%s";           // login:{uid}:{ct}
    private static final String REFRESH_KEY = "refresh:%s";          // refresh:{rtid}
    private static final String TOKEN_VER_KEY = "token:ver:%s";      // token:ver:{uid}

    // ==================== 登录态管理 ====================

    /**
     * 创建登录态
     * @return tokenId
     */
    public String createLogin(Long userId, ClientType clientType) {
        String tokenId = IdUtil.fastSimpleUUID();
        String key = buildLoginKey(userId, clientType);
        
        // 同端互斥：直接覆盖（踢掉同端旧设备）
        redis.opsForValue().set(key, tokenId, 
                jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);
        
        log.info("创建登录态: uid={}, ct={}, tid={}", userId, clientType.getCode(), tokenId);
        return tokenId;
    }

    /**
     * 验证登录态
     * @return true=有效, false=未登录或被踢
     */
    public boolean validateLogin(Long userId, ClientType clientType, String tokenId) {
        String key = buildLoginKey(userId, clientType);
        String storedTokenId = redis.opsForValue().get(key);
        
        if (!StringUtils.hasText(storedTokenId)) {
            log.debug("未登录: uid={}, ct={}", userId, clientType.getCode());
            return false;
        }
        
        if (!tokenId.equals(storedTokenId)) {
            log.debug("被踢下线: uid={}, ct={}, expected={}, actual={}", 
                    userId, clientType.getCode(), storedTokenId, tokenId);
            return false;
        }
        
        return true;
    }

    /**
     * 踢出指定端
     */
    public boolean kickOut(Long userId, ClientType clientType) {
        String key = buildLoginKey(userId, clientType);
        Boolean deleted = redis.delete(key);
        log.info("踢出: uid={}, ct={}, result={}", userId, clientType.getCode(), deleted);
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * 踢出所有端
     */
    public void kickOutAll(Long userId) {
        for (ClientType ct : ClientType.values()) {
            String key = buildLoginKey(userId, ct);
            redis.delete(key);
        }
        log.info("踢出所有端: uid={}", userId);
    }

    // ==================== Refresh Token 管理 ====================

    /**
     * 创建 Refresh Token
     * @return rtid (Refresh Token ID)
     */
    public String createRefreshToken(Long userId, ClientType clientType, String tokenId) {
        String rtid = "rt_" + IdUtil.fastSimpleUUID();
        String key = buildRefreshKey(rtid);
        String value = userId + "|" + clientType.getCode() + "|" + tokenId;
        
        redis.opsForValue().set(key, value, 
                jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);
        
        log.debug("创建RefreshToken: rtid={}", rtid);
        return rtid;
    }

    /**
     * 消费 Refresh Token（一次性）
     * @return [userId, clientType, tokenId] 或 null
     */
    public String[] consumeRefreshToken(String rtid) {
        String key = buildRefreshKey(rtid);
        String value = redis.opsForValue().get(key);
        
        if (!StringUtils.hasText(value)) {
            log.debug("RefreshToken不存在或已使用: rtid={}", rtid);
            return null;
        }
        
        // 立即删除（一次性）
        redis.delete(key);
        
        String[] parts = value.split("\\|");
        if (parts.length != 3) {
            log.warn("RefreshToken格式错误: rtid={}", rtid);
            return null;
        }
        
        log.debug("消费RefreshToken: rtid={}", rtid);
        return parts;
    }

    // ==================== Token 版本管理 ====================

    /**
     * 获取Token版本
     */
    public int getTokenVersion(Long userId) {
        String key = buildTokenVerKey(userId);
        String ver = redis.opsForValue().get(key);
        return ver != null ? Integer.parseInt(ver) : 1;
    }

    /**
     * 递增Token版本（全量失效）
     */
    public int incrementTokenVersion(Long userId) {
        String key = buildTokenVerKey(userId);
        Long newVer = redis.opsForValue().increment(key);
        log.info("Token版本递增: uid={}, ver={}", userId, newVer);
        return newVer != null ? newVer.intValue() : 1;
    }

    /**
     * 验证Token版本
     */
    public boolean validateTokenVersion(Long userId, int tokenVer) {
        int currentVer = getTokenVersion(userId);
        return tokenVer >= currentVer;
    }

    // ==================== 工具方法 ====================

    private String buildLoginKey(Long userId, ClientType clientType) {
        return String.format(LOGIN_KEY, userId, clientType.getCode());
    }

    private String buildRefreshKey(String rtid) {
        return String.format(REFRESH_KEY, rtid);
    }

    private String buildTokenVerKey(Long userId) {
        return String.format(TOKEN_VER_KEY, userId);
    }
}

