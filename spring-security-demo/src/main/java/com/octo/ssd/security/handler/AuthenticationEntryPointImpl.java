package com.octo.ssd.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 设置响应状态码为 401（未认证）
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 设置 Content-Type 并指定 UTF-8
        response.setContentType("application/json;charset=UTF-8");

        // 构造规范化 JSON 响应
        String json = String.format("{\"code\":%d,\"message\":\"%s\"}",
                HttpServletResponse.SC_UNAUTHORIZED,
                "未登录或身份验证失败");

        // 写入响应
        response.getWriter().write(json);
    }
}
