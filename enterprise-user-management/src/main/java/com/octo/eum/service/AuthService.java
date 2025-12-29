package com.octo.eum.service;

import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;

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
     * 用户登出
     *
     * @param token 访问Token
     */
    void logout(String token);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新Token
     * @return 登录响应
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 获取当前用户信息
     *
     * @return 登录响应
     */
    LoginResponse getCurrentUserInfo();
}

