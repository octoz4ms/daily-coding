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
import com.octo.eum.service.LoginTokenService;
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
import java.util.Map;

/**
 * 认证服务
 * 
 * 登录流程：
 * 1. 认证 → 2. 创建登录态(Redis) → 3. 生成双Token → 4. 返回
 * 
 * 刷新流程：
 * 1. 消费RefreshToken(一次性) → 2. 创建新登录态 → 3. 返回新双Token
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginTokenService loginTokenService;
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
            ClientType clientType = getClientType(httpRequest);

            // 3. 创建登录态（同端互斥：自动踢掉同端旧设备）
            String tokenId = loginTokenService.createLogin(loginUser.getUserId(), clientType);

            // 4. 获取Token版本
            int tokenVer = loginTokenService.getTokenVersion(loginUser.getUserId());

            // 5. 生成双Token
            String accessToken = jwtTokenProvider.generateAccessToken(
                    loginUser.getUserId(), loginUser.getUsername(), clientType, tokenId, tokenVer);
            String refreshToken = loginTokenService.createRefreshToken(
                    loginUser.getUserId(), clientType, tokenId);

            // 6. 更新用户登录信息
            userService.updateLoginInfo(loginUser.getUserId(), ip);

            // 7. 记录日志
            loginLog.setUserId(loginUser.getUserId());
            loginLog.setStatus(1);
            loginLog.setMessage("登录成功");
            loginLog.setClientType(clientType.getCode());
            loginLogService.asyncSaveLoginLog(loginLog);

            log.info("登录成功: user={}, ct={}, tid={}", 
                    loginUser.getUsername(), clientType.getCode(), tokenId);

            return buildResponse(loginUser, accessToken, refreshToken, clientType.getCode());

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
                Long userId = jwtTokenProvider.getUserId(token);
                ClientType clientType = jwtTokenProvider.getClientType(token);
                loginTokenService.kickOut(userId, clientType);
                log.info("登出: uid={}, ct={}", userId, clientType.getCode());
            } catch (Exception e) {
                log.warn("登出异常: {}", e.getMessage());
            }
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request) {
        // 1. 消费RefreshToken（一次性）
        String[] parts = loginTokenService.consumeRefreshToken(refreshToken);
        if (parts == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = Long.parseLong(parts[0]);
        ClientType clientType = ClientType.fromCode(parts[1]);
        String oldTokenId = parts[2];

        // 2. 验证旧TokenId（防止RefreshToken被盗后，原设备已重新登录）
        if (!loginTokenService.validateLogin(userId, clientType, oldTokenId)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 3. 获取用户信息
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

        // 4. 创建新登录态（覆盖）
        String newTokenId = loginTokenService.createLogin(userId, clientType);

        // 5. 获取Token版本
        int tokenVer = loginTokenService.getTokenVersion(userId);

        // 6. 生成新双Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                userId, loginUser.getUsername(), clientType, newTokenId, tokenVer);
        String newRefreshToken = loginTokenService.createRefreshToken(userId, clientType, newTokenId);

        log.info("刷新Token: uid={}, ct={}, newTid={}", userId, clientType.getCode(), newTokenId);
        return buildResponse(loginUser, newAccessToken, newRefreshToken, clientType.getCode());
    }

    @Override
    public LoginResponse autoRefreshToken(String accessToken, HttpServletRequest request) {
        // 简化：只返回当前用户信息，Access Token由前端用RefreshToken刷新
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        return buildResponse(loginUser, null, null, null);
    }

    @Override
    public Map<String, Object> checkTokenStatus(String accessToken) {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean valid = jwtTokenProvider.validateToken(accessToken);
            status.put("valid", valid);

            if (valid) {
                Long userId = jwtTokenProvider.getUserId(accessToken);
                ClientType clientType = jwtTokenProvider.getClientType(accessToken);
                String tokenId = jwtTokenProvider.getTokenId(accessToken);
                int tokenVer = jwtTokenProvider.getTokenVersion(accessToken);

                // 检查登录态
                boolean loginValid = loginTokenService.validateLogin(userId, clientType, tokenId);
                status.put("loginValid", loginValid);

                // 检查Token版本
                boolean verValid = loginTokenService.validateTokenVersion(userId, tokenVer);
                status.put("versionValid", verValid);

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

    // ==================== 踢人接口 ====================

    /**
     * 踢出指定端
     */
    public boolean kickOut(Long userId, ClientType clientType) {
        return loginTokenService.kickOut(userId, clientType);
    }

    /**
     * 踢出所有端（修改密码/封号时调用）
     */
    public void kickOutAll(Long userId) {
        loginTokenService.kickOutAll(userId);
        loginTokenService.incrementTokenVersion(userId); // 同时递增版本号
    }

    // ==================== 私有方法 ====================

    private LoginResponse buildResponse(LoginUser loginUser, String accessToken, 
                                        String refreshToken, String clientType) {
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
                .clientType(clientType)
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
}
