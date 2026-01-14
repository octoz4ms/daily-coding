package com.octo.eum.service;

import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 认证服务接口
 *
 * @author octo
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @param ip      登录IP
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request, String ip);

    /**
     * 用户登录
     *
     * @param request     登录请求
     * @param ip          登录IP
     * @param httpRequest HTTP请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request, String ip, HttpServletRequest httpRequest);

    /**
     * 用户登出
     *
     * @param token 访问Token
     */
    void logout(String token);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @param request      HTTP请求
     * @return 登录响应
     */
    LoginResponse refreshToken(String refreshToken, HttpServletRequest request);

    /**
     * 自动刷新Access Token
     *
     * @param accessToken 访问Token
     * @param request     HTTP请求
     * @return 登录响应
     */
    LoginResponse autoRefreshToken(String accessToken, HttpServletRequest request);

    /**
     * 检查Token状态
     *
     * @param accessToken 访问Token
     * @return Token状态信息
     */
    Map<String, Object> checkTokenStatus(String accessToken);

    /**
     * 获取当前用户信息
     *
     * @return 登录响应
     */
    LoginResponse getCurrentUserInfo();
}
