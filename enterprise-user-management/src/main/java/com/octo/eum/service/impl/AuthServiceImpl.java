package com.octo.eum.service.impl;

import com.octo.eum.common.ResultCode;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.security.DeviceType;
import com.octo.eum.security.DeviceUtils;
import com.octo.eum.security.JwtAuthenticationFilter;
import com.octo.eum.security.JwtProperties;
import com.octo.eum.security.JwtTokenProvider;
import com.octo.eum.security.LoginPolicy;
import com.octo.eum.security.LoginUser;
import com.octo.eum.security.SecurityUtils;
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
import java.util.Map;
import java.util.Set;
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
            // 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 识别设备类型
            DeviceType deviceType = httpRequest != null ? 
                    DeviceUtils.getDeviceType(httpRequest) : DeviceType.UNKNOWN;

            // 根据登录策略处理旧Token
            handleLoginPolicy(loginUser.getUserId(), deviceType);

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(loginUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

            // 保存Token到Redis（用于踢人功能）
            saveTokensToRedis(loginUser.getUserId(), deviceType, accessToken, refreshToken);

            // 更新用户登录信息
            userService.updateLoginInfo(loginUser.getUserId(), ip);

            // 记录登录成功日志
            loginLog.setUserId(loginUser.getUserId());
            loginLog.setStatus(1);
            loginLog.setMessage("登录成功 [" + deviceType.getCode() + "]");
            loginLogService.asyncSaveLoginLog(loginLog);

            log.info("用户{}登录成功，设备类型：{}，策略：{}", 
                    loginUser.getUsername(), deviceType.getCode(), jwtProperties.getLoginPolicy());

            return buildLoginResponse(loginUser, accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            log.warn("用户登录失败，密码错误: {}", request.getUsername());
            loginLog.setStatus(0);
            loginLog.setMessage("用户名或密码错误");
            loginLogService.asyncSaveLoginLog(loginLog);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            loginLog.setStatus(0);
            loginLog.setMessage(e.getMessage());
            loginLogService.asyncSaveLoginLog(loginLog);
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
    }

    /**
     * 根据登录策略处理旧Token
     */
    private void handleLoginPolicy(Long userId, DeviceType deviceType) {
        LoginPolicy policy = jwtProperties.getLoginPolicy();

        switch (policy) {
            case SINGLE:
                // 单设备登录：踢掉所有其他设备
                invalidateAllUserTokens(userId);
                break;

            case SAME_TYPE_KICK:
                // 同类型设备互踢：只踢同类型设备
                invalidateDeviceToken(userId, deviceType);
                break;

            case MULTI:
            default:
                // 多端同时在线：不做处理
                break;
        }
    }

    /**
     * 使用户所有设备的Token失效（单设备登录策略）
     */
    private void invalidateAllUserTokens(Long userId) {
        String tokenKeyPrefix = JwtAuthenticationFilter.getTokenKeyPrefix(userId);
        String refreshKeyPrefix = JwtAuthenticationFilter.getRefreshTokenKeyPrefix(userId);

        // 获取所有匹配的Key
        Set<String> tokenKeys = redisTemplate.keys(tokenKeyPrefix + "*");
        Set<String> refreshKeys = redisTemplate.keys(refreshKeyPrefix + "*");

        // 将所有Token加入黑名单
        if (tokenKeys != null) {
            for (String key : tokenKeys) {
                String token = (String) redisTemplate.opsForValue().get(key);
                if (token != null) {
                    addToBlacklist(token);
                }
                redisTemplate.delete(key);
            }
        }

        if (refreshKeys != null) {
            for (String key : refreshKeys) {
                String token = (String) redisTemplate.opsForValue().get(key);
                if (token != null) {
                    addToBlacklist(token);
                }
                redisTemplate.delete(key);
            }
        }

        log.info("用户{}所有设备Token已失效（单设备登录）", userId);
    }

    /**
     * 使指定设备类型的Token失效（同类型互踢策略）
     */
    private void invalidateDeviceToken(Long userId, DeviceType deviceType) {
        String tokenKey = JwtAuthenticationFilter.getTokenKey(userId, deviceType);
        String refreshKey = JwtAuthenticationFilter.getRefreshTokenKey(userId, deviceType);

        // 获取并加入黑名单
        String existingToken = (String) redisTemplate.opsForValue().get(tokenKey);
        if (existingToken != null) {
            addToBlacklist(existingToken);
            redisTemplate.delete(tokenKey);
        }

        String existingRefresh = (String) redisTemplate.opsForValue().get(refreshKey);
        if (existingRefresh != null) {
            addToBlacklist(existingRefresh);
            redisTemplate.delete(refreshKey);
        }

        log.info("用户{}的{}设备Token已失效（同类型互踢）", userId, deviceType.getCode());
    }

    /**
     * 将Token加入黑名单
     */
    private void addToBlacklist(String token) {
        try {
            long expiration = jwtTokenProvider.getTokenRemainingTime(token);
            if (expiration > 0) {
                String blacklistKey = JwtAuthenticationFilter.getBlacklistKey(token);
                redisTemplate.opsForValue().set(blacklistKey, "1", expiration, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("将Token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 保存Token到Redis（用于踢人功能）
     */
    private void saveTokensToRedis(Long userId, DeviceType deviceType, String accessToken, String refreshToken) {
        LoginPolicy policy = jwtProperties.getLoginPolicy();

        // 多端在线：不需要存储（不踢人，无需知道旧Token）
        if (policy == LoginPolicy.MULTI) {
            return;
        }

        String tokenKey;
        String refreshKey;

        if (policy == LoginPolicy.SAME_TYPE_KICK) {
            // 同类型互踢：按设备类型存储
            tokenKey = JwtAuthenticationFilter.getTokenKey(userId, deviceType);
            refreshKey = JwtAuthenticationFilter.getRefreshTokenKey(userId, deviceType);
        } else {
            // 单设备登录：按用户存储
            tokenKey = JwtAuthenticationFilter.getTokenKey(userId);
            refreshKey = JwtAuthenticationFilter.getRefreshTokenKey(userId);
        }

        redisTemplate.opsForValue().set(tokenKey, accessToken,
                jwtTokenProvider.getAccessTokenExpiration(), TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(refreshKey, refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(), TimeUnit.SECONDS);
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null) {
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    // 将Access Token加入黑名单
                    addToBlacklist(token);

                    // 记录登出日志
                    LoginLog loginLog = new LoginLog();
                    loginLog.setUserId(userId);
                    loginLog.setUsername(jwtTokenProvider.getUsernameFromToken(token));
                    loginLog.setType(2);
                    loginLog.setStatus(1);
                    loginLog.setMessage("登出成功");
                    loginLog.setLoginTime(LocalDateTime.now());
                    loginLogService.asyncSaveLoginLog(loginLog);
                }
            } catch (Exception e) {
                log.warn("登出时处理Token失败: {}", e.getMessage());
            }
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken, HttpServletRequest request) {
        // 验证刷新Token格式
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 验证是否为刷新Token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 验证Token版本
        if (!jwtTokenProvider.validateTokenVersion(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 检查Refresh Token是否在黑名单中
        String blacklistKey = JwtAuthenticationFilter.getBlacklistKey(refreshToken);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            log.warn("Refresh Token已被吊销");
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 获取用户信息
        UserVO userVO = userService.getUserByUsername(username);
        if (userVO == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 获取当前用户
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

        // 识别设备类型
        DeviceType deviceType = request != null ? 
                DeviceUtils.getDeviceType(request) : DeviceType.UNKNOWN;

        // 将旧的Refresh Token加入黑名单（单次使用）
        addToBlacklist(refreshToken);

        // 生成新Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(loginUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

        // 保存新Token到Redis
        saveTokensToRedis(loginUser.getUserId(), deviceType, newAccessToken, newRefreshToken);

        log.info("用户{}刷新Token成功", username);
        return buildLoginResponse(loginUser, newAccessToken, newRefreshToken);
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

        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();
        DeviceType deviceType = request != null ? 
                DeviceUtils.getDeviceType(request) : DeviceType.UNKNOWN;

        // 生成新的Access Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(loginUser);

        // 更新Redis中的Access Token（多端在线不存储）
        LoginPolicy policy = jwtProperties.getLoginPolicy();
        if (policy != LoginPolicy.MULTI) {
            String tokenKey = policy == LoginPolicy.SAME_TYPE_KICK ?
                    JwtAuthenticationFilter.getTokenKey(loginUser.getUserId(), deviceType) :
                    JwtAuthenticationFilter.getTokenKey(loginUser.getUserId());

            redisTemplate.opsForValue().set(tokenKey, newAccessToken,
                    jwtTokenProvider.getAccessTokenExpiration(), TimeUnit.SECONDS);
        }

        return buildLoginResponse(loginUser, newAccessToken, null);
    }

    @Override
    public Map<String, Object> checkTokenStatus(String accessToken) {
        Map<String, Object> status = new HashMap<>();

        try {
            boolean isValid = jwtTokenProvider.validateToken(accessToken);
            status.put("valid", isValid);

            if (isValid) {
                long remainingTime = jwtTokenProvider.getTokenRemainingTime(accessToken);
                status.put("remainingTime", remainingTime);
                status.put("expiresAt", jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime());

                int version = jwtTokenProvider.getTokenVersionFromToken(accessToken);
                status.put("version", version);
                status.put("versionValid", version == jwtProperties.getTokenVersion());
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
