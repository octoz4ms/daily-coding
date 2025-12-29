package com.octo.eum.service.impl;

import com.octo.eum.common.ResultCode;
import com.octo.eum.dto.request.LoginRequest;
import com.octo.eum.dto.response.LoginResponse;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.LoginLog;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.security.JwtAuthenticationFilter;
import com.octo.eum.security.JwtTokenProvider;
import com.octo.eum.security.LoginUser;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.service.AuthService;
import com.octo.eum.service.LoginLogService;
import com.octo.eum.service.UserService;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final LoginLogService loginLogService;

    @Override
    public LoginResponse login(LoginRequest request, String ip) {
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

            // 生成Token
            String accessToken = jwtTokenProvider.generateAccessToken(loginUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

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
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 获取用户名
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // 获取用户信息
        UserVO userVO = userService.getUserByUsername(username);
        if (userVO == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 创建一个简单的LoginUser对象用于生成新Token
        LoginUser loginUser = SecurityUtils.getRequiredCurrentUser();

        // 生成新Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(loginUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(loginUser);

        // 保存新Token到Redis
        saveTokenToRedis(loginUser.getUserId(), newAccessToken);

        return buildLoginResponse(loginUser, newAccessToken, newRefreshToken);
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

