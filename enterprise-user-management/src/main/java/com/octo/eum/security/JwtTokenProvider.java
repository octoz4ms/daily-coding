package com.octo.eum.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token提供器
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
        // 确保密钥长度至少32个字符
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            secret = secret + "0".repeat(32 - secret.length());
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问Token
     */
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, null, null);
    }

    /**
     * 生成访问Token（带指纹）
     */
    public String generateAccessToken(UserDetails userDetails, HttpServletRequest request, String fingerprint) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof LoginUser loginUser) {
            claims.put("userId", loginUser.getUserId());
            claims.put("username", loginUser.getUsername());
            claims.put("nickname", loginUser.getNickname());
        }

        // 添加Token版本
        claims.put("version", jwtProperties.getTokenVersion());

        // 添加指纹（如果启用且提供了指纹）
        if (jwtProperties.getEnableFingerprint() && StringUtils.hasText(fingerprint)) {
            claims.put("fingerprint", fingerprint);
        }

        // 添加续签计数
        claims.put("refreshCount", 0);

        // 添加创建时间戳（用于滑动窗口续签）
        claims.put("createdAt", System.currentTimeMillis());

        return generateToken(claims, userDetails.getUsername(), jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails, null, null);
    }

    /**
     * 生成刷新Token（带指纹）
     */
    public String generateRefreshToken(UserDetails userDetails, HttpServletRequest request, String fingerprint) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("version", jwtProperties.getTokenVersion());

        if (userDetails instanceof LoginUser loginUser) {
            claims.put("userId", loginUser.getUserId());
        }

        // 添加指纹（如果启用且提供了指纹）
        if (jwtProperties.getEnableFingerprint() && StringUtils.hasText(fingerprint)) {
            claims.put("fingerprint", fingerprint);
        }

        return generateToken(claims, userDetails.getUsername(), jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * 生成Token
     */
    private String generateToken(Map<String, Object> claims, String subject, Long expiration) {
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
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null;
    }

    /**
     * 获取Token过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 获取Token中的指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 获取Token中的所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token格式是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Token无效: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断Token是否过期
     */
    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 获取Token剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }

    /**
     * 获取访问Token过期时间
     */
    public Long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    /**
     * 获取Token版本
     */
    public Integer getTokenVersionFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object version = claims.get("version");
            if (version instanceof Integer) {
                return (Integer) version;
            } else if (version instanceof Long) {
                return ((Long) version).intValue();
            }
            return 1; // 默认版本
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 获取Token指纹
     */
    public String getFingerprintFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return (String) claims.get("fingerprint");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取续签计数
     */
    public Integer getRefreshCountFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object count = claims.get("refreshCount");
            if (count instanceof Integer) {
                return (Integer) count;
            } else if (count instanceof Long) {
                return ((Long) count).intValue();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取Token创建时间
     */
    public Long getCreatedAtFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            Object createdAt = claims.get("createdAt");
            if (createdAt instanceof Long) {
                return (Long) createdAt;
            } else if (createdAt instanceof Integer) {
                return ((Integer) createdAt).longValue();
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 验证Token版本
     */
    public boolean validateTokenVersion(String token) {
        Integer tokenVersion = getTokenVersionFromToken(token);
        return jwtProperties.getTokenVersion().equals(tokenVersion);
    }

    /**
     * 验证续签次数
     */
    public boolean validateRefreshCount(String token) {
        Integer refreshCount = getRefreshCountFromToken(token);
        return refreshCount < jwtProperties.getMaxRefreshCount();
    }

    /**
     * 检查Token是否需要刷新（基于剩余时间和滑动窗口）
     */
    public boolean shouldRefreshToken(String token) {
        long remainingTime = getTokenRemainingTime(token);
        return remainingTime <= jwtProperties.getAutoRefreshThreshold();
    }

    /**
     * 创建续签的Token（增加续签计数）
     */
    public String createRefreshedToken(String oldToken, UserDetails userDetails, HttpServletRequest request, String fingerprint) {
        Map<String, Object> claims = new HashMap<>();

        // 复制原有声明
        Claims oldClaims = getAllClaimsFromToken(oldToken);
        claims.putAll(oldClaims);

        // 增加续签计数
        Integer currentCount = getRefreshCountFromToken(oldToken);
        claims.put("refreshCount", currentCount + 1);

        // 更新创建时间（用于滑动窗口）
        claims.put("createdAt", System.currentTimeMillis());

        // 重新生成Token
        return generateToken(claims, userDetails.getUsername(), jwtProperties.getAccessTokenExpiration());
    }
}

