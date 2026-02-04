package com.octo.eum.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token工具
 * 
 * Access Token 结构:
 * - uid: userId
 * - sid: sessionId (核心，用于Redis校验)
 * - dt: deviceType
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            secret = secret + "0".repeat(32 - secret.length());
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     *
     * @param userId     用户ID
     * @param username   用户名（作为subject）
     * @param sessionId  会话ID（核心，用于验证）
     * @param deviceType 设备类型
     */
    public String generateAccessToken(Long userId, String username, String sessionId, ClientType deviceType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("sid", sessionId);
        claims.put("dt", deviceType.getCode());

        return buildToken(claims, username, jwtProperties.getAccessTokenExpiration());
    }

    private String buildToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 验证JWT签名和过期
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期");
            return false;
        } catch (JwtException e) {
            log.debug("Token无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户ID
     */
    public Long getUserId(String token) {
        Object uid = getClaims(token).get("uid");
        if (uid instanceof Integer) {
            return ((Integer) uid).longValue();
        }
        return (Long) uid;
    }

    /**
     * 获取用户名
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 获取会话ID
     */
    public String getSessionId(String token) {
        return (String) getClaims(token).get("sid");
    }

    /**
     * 获取设备类型
     */
    public ClientType getDeviceType(String token) {
        String dt = (String) getClaims(token).get("dt");
        return ClientType.fromCode(dt);
    }

    /**
     * 获取剩余时间（秒）
     */
    public long getRemainingTime(String token) {
        try {
            Date exp = getClaims(token).getExpiration();
            return Math.max(0, (exp.getTime() - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    public Long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public Long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
