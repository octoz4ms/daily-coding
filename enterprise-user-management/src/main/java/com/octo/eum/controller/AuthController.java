package com.octo.eum.controller;

import com.octo.eum.common.Result;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.security.ClientType;
import com.octo.eum.security.JwtTokenProvider;
import com.octo.eum.security.LoginUser;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.service.impl.AuthServiceImpl;
import com.octo.eum.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证接口 - 新方案
 *
 * @author octo
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        String ip = IpUtils.getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ip, httpRequest);
        return Result.success(response);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return Result.success();
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestHeader("X-Refresh-Token") String refreshToken,
                                              HttpServletRequest request) {
        LoginResponse response = authService.refreshToken(refreshToken, request);
        return Result.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<LoginResponse> getCurrentUserInfo() {
        LoginResponse response = authService.getCurrentUserInfo();
        return Result.success(response);
    }

    /**
     * 检查Token状态
     */
    @GetMapping("/token-status")
    public Result<Map<String, Object>> checkTokenStatus(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Map<String, Object> status = authService.checkTokenStatus(token);
        return Result.success(status);
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取当前用户所有会话（设备列表）
     */
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> getSessions() {
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        List<Map<String, Object>> sessions = authService.getUserSessions(loginUser.getUserId());
        return Result.success(sessions);
    }

    /**
     * 获取当前用户在线设备数
     */
    @GetMapping("/session-count")
    public Result<Map<String, Object>> getSessionCount() {
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        long count = authService.getUserSessionCount(loginUser.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    /**
     * 踢出指定会话
     */
    @PostMapping("/kick/{sessionId}")
    public Result<Void> kickSession(@PathVariable String sessionId,
                                     @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 防止踢自己
        String currentSessionId = jwtTokenProvider.getSessionId(token);
        if (sessionId.equals(currentSessionId)) {
            return Result.fail(400, "不能踢出当前会话");
        }

        boolean success = authService.kickSession(sessionId);
        return success ? Result.success() : Result.fail(400, "会话不存在");
    }

    /**
     * 踢出当前用户所有会话（保留当前）
     */
    @PostMapping("/kick-others")
    public Result<Map<String, Object>> kickOthers(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        String currentSessionId = jwtTokenProvider.getSessionId(token);

        // 获取所有会话并踢出除当前外的
        List<Map<String, Object>> sessions = authService.getUserSessions(loginUser.getUserId());
        int count = 0;
        for (Map<String, Object> session : sessions) {
            String sid = (String) session.get("sessionId");
            if (!currentSessionId.equals(sid)) {
                if (authService.kickSession(sid)) {
                    count++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("message", "已踢出 " + count + " 个设备");
        return Result.success(result);
    }

    /**
     * 踢出同类型设备
     */
    @PostMapping("/kick-same-type/{deviceType}")
    public Result<Void> kickSameType(@PathVariable String deviceType,
                                      @RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        ClientType type = ClientType.fromCode(deviceType);
        ClientType currentType = jwtTokenProvider.getDeviceType(token);

        // 如果踢的是当前设备类型，提示需要用 kick-others
        if (type == currentType) {
            return Result.fail(400, "踢同类型设备会踢掉自己，请使用 kick-others");
        }

        authService.kickSameTypeSessions(loginUser.getUserId(), type);
        return Result.success();
    }
}
