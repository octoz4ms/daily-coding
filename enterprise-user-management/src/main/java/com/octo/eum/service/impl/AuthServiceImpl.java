package com.octo.eum.service.impl;

import com.octo.eum.common.ResultCode;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.security.JwtAuthenticationFilter;
import com.octo.eum.security.JwtProperties;
import com.octo.eum.security.JwtTokenProvider;
import com.octo.eum.security.LoginUser;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.security.TokenFingerprintUtils;
import com.octo.eum.service.AuthService;
import com.octo.eum.service.LoginLogService;
import com.octo.eum.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final LoginLogService loginLogService;
    private final TokenFingerprintUtils fingerprintUtils;

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        return login(request, ip, null);
    }

    @Override
    public LoginResponse login(LoginRequest request, String ip, HttpServletRequest httpRequest) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(request.getUsername());
        loginLog.setType(1); // 登录
        loginLog.setIp(ip);
        loginLog.setLoginTime(LocalDateTime.now());

        try {
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 设置认证信息到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取登录用户
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 生成Token（带指纹）
            String fingerprint = null;
            if (jwtProperties.getEnableFingerprint() && httpRequest != null) {
                fingerprint = fingerprintUtils.generateFingerprint(httpRequest);
            }

            String accessToken = jwtTokenProvider.generateAccessToken(loginUser, httpRequest, fingerprint);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser, httpRequest, fingerprint);

            // 单设备登录处理
            if (jwtProperties.getSingleDeviceLogin()) {
                invalidateOtherDevices(loginUser.getUserId());
            }

            // 保存Token到Redis
            saveTokenToRedis(loginUser.getUserId(), accessToken);

            // 更新用户登录信息
            userService.updateLoginInfo(loginUser.getUserId(), ip);

            // 记录登录成功日志
            loginLog.setUserId(loginUser.getUserId());
            loginLog.setStatus(1); // 成功
            loginLog.setMessage("登录成功");
            loginLogService.asyncSaveLoginLog(loginLog);

            // 构建响应
            return buildLoginResponse(loginUser, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            log.warn("用户登录失败，密码错误: {}", request.getUsername());
            // 记录登录失败日志
            loginLog.setStatus(0); // 失败
            loginLog.setMessage("用户名或密码错误");
            loginLogService.asyncSaveLoginLog(loginLog);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            // 记录登录失败日志
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

        if (token != null) {
            try {
                // 获取Token剩余有效时间
                long expiration = jwtTokenProvider.getTokenRemainingTime(token);
                if (expiration > 0) {
                    // 将Token加入黑名单
                    String blacklistKey = JwtAuthenticationFilter.getBlacklistKey(token);
                    redisTemplate.opsForValue().set(blacklistKey, "1", expiration, TimeUnit.SECONDS);
                }

                // 获取用户ID，删除Redis中的Token
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    String tokenKey = JwtAuthenticationFilter.getTokenKey(userId);
                    redisTemplate.delete(tokenKey);

                    // 记录登出日志
                    LoginLog loginLog = new LoginLog();
                    loginLog.setUserId(userId);
                    loginLog.setUsername(jwtTokenProvider.getUsernameFromToken(token));
                    loginLog.setType(2); // 登出
                    loginLog.setStatus(1);
                    loginLog.setMessage("登出成功");
                    loginLog.setLoginTime(LocalDateTime.now());
                    loginLogService.asyncSaveLoginLog(loginLog);
                }
            } catch (Exception e) {
                log.warn("登出时处理Token失败: {}", e.getMessage());
            }
        }

        // 清除SecurityContext
        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request) {
        // 验证刷新Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 验证Token版本
        if (!jwtTokenProvider.validateTokenVersion(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 获取用户名和用户ID
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 获取用户信息
        UserVO userVO = userService.getUserByUsername(username);
        if (userVO == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证指纹（如果启用）
        if (jwtProperties.getEnableFingerprint() && request != null) {
            String storedFingerprint = jwtTokenProvider.getFingerprintFromToken(refreshToken);
            if (!fingerprintUtils.validateFingerprint(request, storedFingerprint)) {
                throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
            }
        }

        // 并发刷新保护
        String refreshLockKey = "refresh:lock:" + userId;
        boolean lockAcquired = false;
        try {
            if (jwtProperties.getEnableConcurrentRefreshProtection()) {
                lockAcquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(refreshLockKey, "1",
                    jwtProperties.getRefreshLockTimeout(), TimeUnit.SECONDS));
                if (!lockAcquired) {
                    throw new BusinessException(ResultCode.TOKEN_REFRESH_IN_PROGRESS);
                }
            }

            // 创建LoginUser对象用于生成新Token
            LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

            // 生成新Token（带指纹）
            String fingerprint = null;
            if (jwtProperties.getEnableFingerprint() && request != null) {
                fingerprint = fingerprintUtils.generateFingerprint(request);
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(loginUser, request, fingerprint);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(loginUser, request, fingerprint);

            // 保存新Token到Redis
            saveTokenToRedis(loginUser.getUserId(), newAccessToken);

            return buildLoginResponse(loginUser, newAccessToken, newRefreshToken);
        } finally {
            if (lockAcquired) {
                redisTemplate.delete(refreshLockKey);
            }
        }
    }

    @Override
    public LoginResponse autoRefreshToken(String accessToken, HttpServletRequest request) {
        // 验证Token
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ResultCode.ACCESS_TOKEN_INVALID);
        }

        // 验证版本
        if (!jwtTokenProvider.validateTokenVersion(accessToken)) {
            throw new BusinessException(ResultCode.ACCESS_TOKEN_INVALID);
        }

        // 验证续签次数
        if (!jwtTokenProvider.validateRefreshCount(accessToken)) {
            throw new BusinessException(ResultCode.TOKEN_REFRESH_LIMIT_EXCEEDED);
        }

        // 验证指纹
        if (jwtProperties.getEnableFingerprint()) {
            String storedFingerprint = jwtTokenProvider.getFingerprintFromToken(accessToken);
            if (!fingerprintUtils.validateFingerprint(request, storedFingerprint)) {
                throw new BusinessException(ResultCode.ACCESS_TOKEN_INVALID);
            }
        }

        // 获取当前用户
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

        // 并发刷新保护
        String refreshLockKey = "auto_refresh:lock:" + loginUser.getUserId();
        boolean lockAcquired = false;
        try {
            if (jwtProperties.getEnableConcurrentRefreshProtection()) {
                lockAcquired = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(refreshLockKey, "1",
                    jwtProperties.getRefreshLockTimeout(), TimeUnit.SECONDS));
                if (!lockAcquired) {
                    throw new BusinessException(ResultCode.TOKEN_REFRESH_IN_PROGRESS);
                }
            }

            // 生成新的Access Token
            String fingerprint = jwtProperties.getEnableFingerprint() ? fingerprintUtils.generateFingerprint(request) : null;
            String newAccessToken = jwtTokenProvider.createRefreshedToken(accessToken, loginUser, request, fingerprint);

            // 保存新Token到Redis
            saveTokenToRedis(loginUser.getUserId(), newAccessToken);

            return buildLoginResponse(loginUser, newAccessToken, null);
        } finally {
            if (lockAcquired) {
                redisTemplate.delete(refreshLockKey);
            }
        }
    }

    @Override
    public Map<String, Object> checkTokenStatus(String accessToken) {
        Map<String, Object> status = new HashMap<>();

        try {
            // 基础验证
            boolean isValid = jwtTokenProvider.validateToken(accessToken);
            status.put("valid", isValid);

            if (isValid) {
                // 剩余时间
                long remainingTime = jwtTokenProvider.getTokenRemainingTime(accessToken);
                status.put("remainingTime", remainingTime);

                // 是否需要刷新
                boolean shouldRefresh = jwtTokenProvider.shouldRefreshToken(accessToken);
                status.put("shouldRefresh", shouldRefresh);

                // 续签次数
                int refreshCount = jwtTokenProvider.getRefreshCountFromToken(accessToken);
                status.put("refreshCount", refreshCount);

                // 版本信息
                int version = jwtTokenProvider.getTokenVersionFromToken(accessToken);
                status.put("version", version);
                status.put("currentVersion", jwtProperties.getTokenVersion());
                status.put("versionValid", version == jwtProperties.getTokenVersion());

                // 过期时间
                status.put("expiresAt", jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime());
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
        UserVO userVO = convertToUserVO(loginUser);

        return LoginResponse.builder()
                .user(userVO)
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }

    /**
     * 保存Token到Redis
     */
    private void saveTokenToRedis(Long userId, String token) {
        String key = JwtAuthenticationFilter.getTokenKey(userId);
        redisTemplate.opsForValue().set(key, token,
                jwtTokenProvider.getAccessTokenExpiration(), TimeUnit.SECONDS);
    }

    /**
     * 单设备登录：使其他设备Token失效
     */
    private void invalidateOtherDevices(Long userId) {
        // 获取当前设备标识（可以基于指纹或其他方式）
        String currentDeviceKey = "device:" + userId + ":current";

        // 生成新的设备标识
        String deviceId = generateDeviceId();

        // 将旧的Token加入黑名单
        String tokenKey = JwtAuthenticationFilter.getTokenKey(userId);
        String existingToken = (String) redisTemplate.opsForValue().get(tokenKey);

        if (existingToken != null) {
            // 将现有Token加入黑名单，剩余时间作为黑名单过期时间
            long remainingTime = jwtTokenProvider.getTokenRemainingTime(existingToken);
            if (remainingTime > 0) {
                String blacklistKey = JwtAuthenticationFilter.getBlacklistKey(existingToken);
                redisTemplate.opsForValue().set(blacklistKey, deviceId, remainingTime, TimeUnit.SECONDS);
            }
        }

        // 更新当前设备标识
        redisTemplate.opsForValue().set(currentDeviceKey, deviceId, jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);

        // 记录设备切换日志
        log.info("用户{}切换设备登录，原Token已失效", userId);
    }

    /**
     * 生成设备ID
     */
    private String generateDeviceId() {
        return String.valueOf(System.currentTimeMillis()) + "-" + String.valueOf(Math.random()).substring(2, 8);
    }


    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(LoginUser loginUser, String accessToken, String refreshToken) {
        UserVO userVO = convertToUserVO(loginUser);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .user(userVO)
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }

    /**
     * 转换为UserVO
     */
    private UserVO convertToUserVO(LoginUser loginUser) {
        UserVO userVO = new UserVO();
        userVO.setId(loginUser.getUserId());
        userVO.setUsername(loginUser.getUsername());
        userVO.setNickname(loginUser.getNickname());
        userVO.setAvatar(loginUser.getAvatar());
        userVO.setStatus(loginUser.getStatus());
        return userVO;
    }
}

