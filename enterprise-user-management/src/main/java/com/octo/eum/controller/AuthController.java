package com.octo.eum.controller;

import com.octo.eum.common.Result;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.security.ClientType;
import com.octo.eum.security.JwtTokenProvider;
import com.octo.eum.security.LoginUser;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.service.LoginTokenService;
import com.octo.eum.service.impl.AuthServiceImpl;
import com.octo.eum.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口
 *
 * @author octo
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final LoginTokenService loginTokenService;
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

    // ==================== 踢人接口 ====================

    /**
     * 踢出当前用户的其他端
     */
    @PostMapping("/kick/{clientType}")
    public Result<Void> kickClient(@PathVariable String clientType) {
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        ClientType ct = ClientType.fromCode(clientType);
        boolean success = loginTokenService.kickOut(loginUser.getUserId(), ct);
        return success ? Result.success() : Result.fail(400, "未找到该端登录");
    }

    /**
     * 踢出当前用户所有端（保留当前）
     */
    @PostMapping("/kick-others")
    public Result<Map<String, Object>> kickOthers(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        ClientType currentCt = jwtTokenProvider.getClientType(token);

        int count = 0;
        for (ClientType ct : ClientType.values()) {
            if (ct != currentCt && ct != ClientType.UNKNOWN) {
                if (loginTokenService.kickOut(loginUser.getUserId(), ct)) {
                    count++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("message", "已踢出 " + count + " 个端");
        return Result.success(result);
    }
}
