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
     * Redis中Token的Key前缀
     */
    private static final String TOKEN_PREFIX = "auth:token:";

    /**
     * Redis中黑名单Token的Key前缀
     */
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 从请求中获取Token
            String token = getTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                // 检查Token是否在黑名单中（已登出）
                if (isTokenBlacklisted(token)) {
                    log.debug("Token已被加入黑名单");
                } else if (jwtTokenProvider.validateToken(token)) {
                    // 从Token中获取用户名
                    String username = jwtTokenProvider.getUsernameFromToken(token);

                    // 加载用户信息
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 验证Token是否有效
                    if (jwtTokenProvider.validateToken(token, userDetails)) {
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置到SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("用户认证成功: {}", username);
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

    /**
     * 获取Token在Redis中的Key
     */
    public static String getTokenKey(Long userId) {
        return TOKEN_PREFIX + userId;
    }

    /**
     * 获取黑名单Token的Key
     */
    public static String getBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }
}

