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
 * - userId
 * - clientType
 * - tokenId (核心，用于Redis校验)
 * - ver (Token版本)
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
     */
    public String generateAccessToken(Long userId, String username, ClientType clientType, 
                                       String tokenId, int tokenVer) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("ct", clientType.getCode());
        claims.put("tid", tokenId);
        claims.put("ver", tokenVer);

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
     * 获取客户端类型
     */
    public ClientType getClientType(String token) {
        String ct = (String) getClaims(token).get("ct");
        return ClientType.fromCode(ct);
    }

    /**
     * 获取TokenId
     */
    public String getTokenId(String token) {
        return (String) getClaims(token).get("tid");
    }

    /**
     * 获取Token版本
     */
    public int getTokenVersion(String token) {
        Object ver = getClaims(token).get("ver");
        if (ver instanceof Integer) {
            return (Integer) ver;
        }
        return ((Long) ver).intValue();
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

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
