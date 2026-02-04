package com.octo.eum.service.impl;

import com.octo.eum.common.ResultCode;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.security.*;
import com.octo.eum.service.AuthService;
import com.octo.eum.service.LoginLogService;
import com.octo.eum.service.SessionService;
import com.octo.eum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务 - 新方案实现
 *
 * Redis 结构：
 * - session:{sessionId}               → Hash  会话详情
 * - refresh:{rtid}                    → String → sessionId
 * - user:sessions:{userId}            → ZSet  用户所有会话（按时间排序）
 * - user:device:{userId}:{deviceType} → Set   同类型设备会话
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final UserService userService;
    private final LoginLogService loginLogService;

    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        return login(request, ip, null);
    }

    @Override
    public LoginResponse login(LoginRequest request, String ip, HttpServletRequest httpRequest) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(request.getUsername());
        loginLog.setType(1);
        loginLog.setIp(ip);
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            // 1. 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 2. 获取客户端类型
            ClientType deviceType = getClientType(httpRequest);
            String deviceName = getDeviceName(httpRequest);

            // 3. 创建会话（根据策略自动踢人）
            String sessionId = sessionService.createSession(
                    loginUser.getUserId(), deviceType, ip, deviceName);

            // 4. 生成双Token
            String accessToken = jwtTokenProvider.generateAccessToken(
                    loginUser.getUserId(), loginUser.getUsername(), sessionId, deviceType);
            String refreshToken = sessionService.createRefreshToken(sessionId);

            // 5. 更新用户登录信息
            userService.updateLoginInfo(loginUser.getUserId(), ip);

            // 6. 记录日志
            loginLog.setUserId(loginUser.getUserId());
            loginLog.setStatus(1);
            loginLog.setMessage("登录成功");
            loginLog.setClientType(deviceType.getCode());
            loginLog.setSessionId(sessionId);
            loginLog.setDeviceName(deviceName);
            loginLogService.asyncSaveLoginLog(loginLog);

            log.info("登录成功: user={}, deviceType={}, sessionId={}",
                    loginUser.getUsername(), deviceType.getCode(), sessionId);

            return buildResponse(loginUser, accessToken, refreshToken, sessionId, deviceType);

        } catch (BadCredentialsException e) {
            loginLog.setStatus(0);
            loginLog.setMessage("密码错误");
            loginLogService.asyncSaveLoginLog(loginLog);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage(), e);
            loginLog.setStatus(0);
            loginLog.setMessage(e.getMessage());
            loginLogService.asyncSaveLoginLog(loginLog);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (StringUtils.hasText(token)) {
            try {
                String sessionId = jwtTokenProvider.getSessionId(token);
                sessionService.kickSession(sessionId);
                log.info("登出: sessionId={}", sessionId);
            } catch (Exception e) {
                log.warn("登出异常: {}", e.getMessage());
            }
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request) {
        // 1. 消费RefreshToken（一次性）
        String sessionId = sessionService.consumeRefreshToken(refreshToken);
        if (sessionId == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 验证会话是否有效
        if (!sessionService.validateSession(sessionId)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 3. 获取用户信息
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        ClientType deviceType = jwtTokenProvider.getDeviceType(
                request.getHeader("Authorization").substring(7));

        // 4. 生成新的RefreshToken（sessionId保持不变）
        String newRefreshToken = sessionService.createRefreshToken(sessionId);

        // 5. 生成新的AccessToken
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                loginUser.getUserId(), loginUser.getUsername(), sessionId, deviceType);

        log.info("刷新Token: sessionId={}", sessionId);
        return buildResponse(loginUser, newAccessToken, newRefreshToken, sessionId, deviceType);
    }

    @Override
    public LoginResponse autoRefreshToken(String accessToken, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        return buildResponse(loginUser, null, null, null, null);
    }

    @Override
    public Map<String, Object> checkTokenStatus(String accessToken) {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean valid = jwtTokenProvider.validateToken(accessToken);
            status.put("valid", valid);

            if (valid) {
                String sessionId = jwtTokenProvider.getSessionId(accessToken);
                boolean sessionValid = sessionService.validateSession(sessionId);
                status.put("sessionValid", sessionValid);

                long remaining = jwtTokenProvider.getRemainingTime(accessToken);
                status.put("remainingTime", remaining);
                status.put("needRefresh", remaining < 300);
            }
        } catch (Exception e) {
            status.put("valid", false);
            status.put("error", e.getMessage());
        }

        return status;
    }

    @Override
    public LoginResponse getCurrentUserInfo() {
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

        UserVO userVO = new UserVO();
        userVO.setId(loginUser.getUserId());
        userVO.setUsername(loginUser.getUsername());
        userVO.setNickname(loginUser.getNickname());
        userVO.setAvatar(loginUser.getAvatar());
        userVO.setStatus(loginUser.getStatus());

        return LoginResponse.builder()
                .user(userVO)
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取用户所有会话
     */
    public List<Map<String, Object>> getUserSessions(Long userId) {
        return sessionService.getUserSessions(userId);
    }

    /**
     * 获取用户在线设备数
     */
    public long getUserSessionCount(Long userId) {
        return sessionService.getUserSessionCount(userId);
    }

    /**
     * 踢出指定会话
     */
    public boolean kickSession(String sessionId) {
        return sessionService.kickSession(sessionId);
    }

    /**
     * 踢出用户所有会话（修改密码/封号时调用）
     */
    public void kickAllSessions(Long userId) {
        sessionService.kickAllSessions(userId);
    }

    /**
     * 踢出用户同类型设备
     */
    public void kickSameTypeSessions(Long userId, ClientType deviceType) {
        sessionService.kickSameTypeSessions(userId, deviceType);
    }

    // ==================== 私有方法 ====================

    private LoginResponse buildResponse(LoginUser loginUser, String accessToken,
                                        String refreshToken, String sessionId, ClientType deviceType) {
        UserVO userVO = new UserVO();
        userVO.setId(loginUser.getUserId());
        userVO.setUsername(loginUser.getUsername());
        userVO.setNickname(loginUser.getNickname());
        userVO.setAvatar(loginUser.getAvatar());
        userVO.setStatus(loginUser.getStatus());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userVO)
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .sessionId(sessionId)
                .deviceType(deviceType != null ? deviceType.getCode() : null)
                .build();
    }

    private ClientType getClientType(HttpServletRequest request) {
        if (request != null) {
            String ct = request.getHeader(CLIENT_TYPE_HEADER);
            if (StringUtils.hasText(ct)) {
                return ClientType.fromCode(ct);
            }
            return ClientType.fromUserAgent(request.getHeader("User-Agent"));
        }
        return ClientType.UNKNOWN;
    }

    private String getDeviceName(HttpServletRequest request) {
        if (request != null) {
            String deviceName = request.getHeader("X-Device-Name");
            if (StringUtils.hasText(deviceName)) {
                return deviceName;
            }
            // 简单从 User-Agent 推断
            String ua = request.getHeader("User-Agent");
            if (ua != null) {
                if (ua.contains("iPhone")) return "iPhone";
                if (ua.contains("iPad")) return "iPad";
                if (ua.contains("Android")) return "Android";
                if (ua.contains("Windows")) return "Windows PC";
                if (ua.contains("Mac")) return "Mac";
            }
        }
        return "Unknown";
    }
}
