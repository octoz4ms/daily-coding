package com.octo.eum.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token刷新响应过滤器
 * 在响应中添加Token刷新提示头
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 1) // 确保在最后执行
public class TokenRefreshResponseFilter extends OncePerRequestFilter {

    private static final String TOKEN_REFRESH_HEADER = "X-Token-Refresh";
    private static final String TOKEN_REFRESH_NEEDED = "refresh-needed";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 执行请求
        filterChain.doFilter(request, response);

        // 检查是否需要添加Token刷新提示
        Object refreshFlag = request.getAttribute(TOKEN_REFRESH_HEADER);
        if (TOKEN_REFRESH_NEEDED.equals(refreshFlag)) {
            response.setHeader(TOKEN_REFRESH_HEADER, TOKEN_REFRESH_NEEDED);

            // 可选：添加其他提示信息
            response.setHeader("X-Token-Refresh-Message", "Token即将过期，建议刷新");
            response.setHeader("X-Token-Refresh-Url", "/api/auth/auto-refresh");

            log.debug("已添加Token刷新提示到响应头");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 不对认证接口添加刷新提示
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/login") ||
               path.startsWith("/static/");
    }
}
