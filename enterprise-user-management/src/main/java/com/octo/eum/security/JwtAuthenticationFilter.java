package com.octo.eum.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
    private final TokenFingerprintUtils fingerprintUtils;
    private final ThreadPoolTaskExecutor asyncTaskExecutor;

    // 异步刷新相关常量
    private static final String TOKEN_REFRESH_HEADER = "X-Token-Refresh";
    private static final String TOKEN_REFRESH_NEEDED = "refresh-needed";
    private static final String TOKEN_AUTO_REFRESH = "auto-refresh";
    private static final String ASYNC_REFRESH_KEY_PREFIX = "async:refresh:";

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
                    // 验证Token版本
                    if (!jwtTokenProvider.validateTokenVersion(token)) {
                        log.warn("Token版本已过期，需要重新登录");
                    } else {
                        // 从Token中获取用户名
                        String username = jwtTokenProvider.getUsernameFromToken(token);

                        // 加载用户信息
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // 验证Token是否有效
                        if (jwtTokenProvider.validateToken(token, userDetails)) {
                            // 验证指纹（如果启用）
                            boolean fingerprintValid = true;
                            if (jwtProperties.getEnableFingerprint()) {
                                String storedFingerprint = jwtTokenProvider.getFingerprintFromToken(token);
                                fingerprintValid = fingerprintUtils.validateFingerprint(request, storedFingerprint);
                            }

                            if (fingerprintValid) {
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

                                // 检查是否需要自动刷新Token
                                if (jwtProperties.getEnableSlidingRefresh() && jwtTokenProvider.shouldRefreshToken(token)) {
                                    handleTokenRefresh(token, request, username);
                                }
                            } else {
                                log.warn("Token指纹校验失败，拒绝访问: {}", username);
                            }
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

    /**
     * 处理Token刷新 - 大厂级解决方案
     *
     * 支持三种刷新策略：
     * 1. 响应头提示：让前端主动刷新
     * 2. 异步刷新：后台自动刷新，不阻塞请求
     * 3. 预刷新：直接在响应中返回新Token
     */
    private void handleTokenRefresh(String token, HttpServletRequest request, String username) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) return;

        // 方案1：添加响应头提示前端刷新（推荐）
        addRefreshHeaderToResponse(request);

        // 方案2：异步刷新Token（可选，需要配置线程池）
        // asyncRefreshToken(token, userId, username, request);

        log.debug("Token即将过期，添加刷新提示: {}", username);
    }

    /**
     * 在响应头中添加Token刷新提示
     * 前端可以检查这个响应头来决定是否刷新Token
     */
    private void addRefreshHeaderToResponse(HttpServletRequest request) {
        // 使用RequestContextHolder或其他方式获取Response
        // 这里我们设置一个请求属性，让后续的响应处理器处理
        request.setAttribute(TOKEN_REFRESH_HEADER, TOKEN_REFRESH_NEEDED);
    }

    /**
     * 异步刷新Token（可选方案）
     * 优点：不阻塞当前请求，提升响应速度
     * 缺点：增加了复杂性，需要处理并发和失败情况
     */
    private void asyncRefreshToken(String token, Long userId, String username, HttpServletRequest request) {
        // 生成异步刷新Key，防止重复刷新
        String asyncRefreshKey = ASYNC_REFRESH_KEY_PREFIX + userId;
        String refreshValue = token + ":" + System.currentTimeMillis();

        // 使用Redis的SET NX来防止并发刷新
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(asyncRefreshKey, refreshValue, 60, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("用户{}的Token正在异步刷新中，跳过", username);
            return;
        }

        // 提交异步任务
        asyncTaskExecutor.execute(() -> {
            try {
                performAsyncTokenRefresh(token, userId, username, request);
            } catch (Exception e) {
                log.error("异步刷新Token失败: {}", username, e);
            } finally {
                // 清理异步刷新标记
                redisTemplate.delete(asyncRefreshKey);
            }
        });
    }

    /**
     * 执行异步Token刷新
     */
    private void performAsyncTokenRefresh(String token, Long userId, String username, HttpServletRequest request) {
        try {
            log.info("开始异步刷新用户{}的Token", username);

            // 获取用户信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 生成指纹
            String fingerprint = jwtProperties.getEnableFingerprint() ?
                fingerprintUtils.generateFingerprint(request) : null;

            // 生成新的Access Token
            String newAccessToken = jwtTokenProvider.createRefreshedToken(token, userDetails, request, fingerprint);

            // 更新Redis中的Token
            String tokenKey = getTokenKey(userId);
            redisTemplate.opsForValue().set(tokenKey, newAccessToken,
                jwtProperties.getAccessTokenExpiration(), TimeUnit.SECONDS);

            log.info("用户{}的Token异步刷新成功", username);

        } catch (Exception e) {
            log.error("异步刷新Token异常: {}", username, e);
        }
    }

    /**
     * 预刷新Token（同步方案）
     * 优点：简单直接
     * 缺点：可能影响响应时间，增加服务端负载
     */
    private void preRefreshToken(String token, HttpServletRequest request, HttpServletResponse response, String username) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            if (userId == null) return;

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String fingerprint = jwtProperties.getEnableFingerprint() ?
                fingerprintUtils.generateFingerprint(request) : null;

            String newAccessToken = jwtTokenProvider.createRefreshedToken(token, userDetails, request, fingerprint);

            // 更新Redis
            String tokenKey = getTokenKey(userId);
            redisTemplate.opsForValue().set(tokenKey, newAccessToken,
                jwtProperties.getAccessTokenExpiration(), TimeUnit.SECONDS);

            // 在响应头中返回新Token
            response.setHeader("X-New-Access-Token", newAccessToken);
            response.setHeader("X-Token-Refreshed", "true");

            log.debug("预刷新用户{}的Token成功", username);

        } catch (Exception e) {
            log.error("预刷新Token失败: {}", username, e);
        }
    }
}

