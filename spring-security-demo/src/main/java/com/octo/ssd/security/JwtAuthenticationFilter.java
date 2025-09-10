package com.octo.ssd.security;

import com.octo.ssd.entity.User;
import com.octo.ssd.utils.RedisUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Resource
    private RedisUtil redisUtil;

    /**
     * JWT认证过滤器，用于验证请求中的JWT令牌并设置Spring Security上下文
     *
     * @param request     HTTP请求对象
     * @param response    HTTP响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet异常
     * @throws IOException      IO异常
     */
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {
        // 从请求中获取JWT令牌
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && JwtUtils.validateToken(token)) {
            // 从令牌中解析出用户名
            String username = JwtUtils.getUsername(token);

            // 根据用户名加载用户详细信息，后期替换为redis获取
            User user = redisUtil.get("login:user:" + token, User.class);

            // 用户信息不存在时，可能是登出、删除、禁用或者不可用
            if (user != null) {
                // 创建认证对象，包含用户信息和权限列表
                LoginUser loginUser = new LoginUser(user);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
                // 将认证信息存储到安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT令牌
     *
     * @param request HTTP请求对象
     * @return 提取到的JWT令牌，如果不存在或格式不正确则返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
