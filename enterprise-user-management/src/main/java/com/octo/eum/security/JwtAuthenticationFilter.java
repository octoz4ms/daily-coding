package com.octo.eum.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CustomUserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis Key 前缀
     */
    private static final String TOKEN_PREFIX = "auth:token:";
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 检查Token是否在黑名单中
                if (isTokenBlacklisted(token)) {
                    log.debug("Token已被加入黑名单");
                } else if (jwtTokenProvider.validateToken(token)) {
                    // 验证Token版本
                    if (!jwtTokenProvider.validateTokenVersion(token)) {
                        log.warn("Token版本已过期，需要重新登录");
                    } else {
                        String username = jwtTokenProvider.getUsernameFromToken(token);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (jwtTokenProvider.validateToken(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("用户认证成功: {}", username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtProperties.getTokenPrefix())) {
            return bearerToken.substring(jwtProperties.getTokenPrefix().length());
        }
        return null;
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ==================== Redis Key 生成方法 ====================

    /**
     * 获取黑名单Key
     */
    public static String getBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    /**
     * 获取Access Token Key（按用户+设备类型）
     */
    public static String getTokenKey(Long userId, DeviceType deviceType) {
        return TOKEN_PREFIX + userId + ":" + deviceType.getCode();
    }

    /**
     * 获取Access Token Key（单设备模式，不区分设备类型）
     */
    public static String getTokenKey(Long userId) {
        return TOKEN_PREFIX + userId;
    }

    /**
     * 获取Refresh Token Key（按用户+设备类型）
     */
    public static String getRefreshTokenKey(Long userId, DeviceType deviceType) {
        return REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getCode();
    }

    /**
     * 获取Refresh Token Key（单设备模式）
     */
    public static String getRefreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    /**
     * 获取Token Key前缀（用于模糊删除）
     */
    public static String getTokenKeyPrefix(Long userId) {
        return TOKEN_PREFIX + userId;
    }

    /**
     * 获取Refresh Token Key前缀（用于模糊删除）
     */
    public static String getRefreshTokenKeyPrefix(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
