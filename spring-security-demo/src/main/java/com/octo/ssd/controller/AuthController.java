package com.octo.ssd.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.octo.ssd.common.Result;
import com.octo.ssd.common.ResultCode;
import com.octo.ssd.config.JwtProperties;
import com.octo.ssd.dto.LoginRequest;
import com.octo.ssd.dto.LoginResponse;
import com.octo.ssd.dto.RegisterRequest;
import com.octo.ssd.entity.User;
import com.octo.ssd.exception.BusinessException;
import com.octo.ssd.security.JwtUtils;
import com.octo.ssd.security.LoginUser;
import com.octo.ssd.service.IUserService;
import com.octo.ssd.utils.RedisUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 * 处理用户登录、注册、退出等认证相关操作
 *
 * @author octo
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private JwtProperties jwtProperties;

    @Resource
    private IUserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @return 登录结果，包含token和用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 使用AuthenticationManager进行身份验证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 验证通过后，从Authentication中获取登录用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();

        // 生成JWT token
        String token = JwtUtils.generateToken(loginUser.getUsername());

        // 存储token -> redis
        redisUtil.set("login:user:" + token,
                user,
                jwtProperties.getAccessExpire(),
                TimeUnit.MILLISECONDS);

        // 构建登录响应
        LoginResponse response = LoginResponse.from(user, token, (long) jwtProperties.getAccessExpire());

        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 校验两次密码是否一致
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, registerRequest.getUsername());
        if (userService.count(queryWrapper) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建新用户
        User user = new User();
        user.setUserNo(generateUserNo());
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        userService.save(user);

        return Result.success("注册成功", null);
    }

    /**
     * 用户退出登录
     *
     * @param token 请求头中的Authorization
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return Result.success("退出成功", null);
        }

        String jwtToken = token.substring(7);
        redisUtil.delete("login:user:" + jwtToken);
        return Result.success("退出成功", null);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param token 请求头中的Authorization
     * @return 当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String jwtToken = token.substring(7);
        User user = redisUtil.get("login:user:" + jwtToken, User.class);

        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 刷新Token
     *
     * @param token 请求头中的Authorization
     * @return 新的token
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refreshToken(@RequestHeader("Authorization") String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String oldToken = token.substring(7);

        // 验证旧token是否有效
        if (!JwtUtils.validateToken(oldToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 从Redis获取用户信息
        User user = redisUtil.get("login:user:" + oldToken, User.class);
        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 生成新token
        String newToken = JwtUtils.generateToken(user.getUsername());

        // 删除旧token
        redisUtil.delete("login:user:" + oldToken);

        // 存储新token
        redisUtil.set("login:user:" + newToken,
                user,
                jwtProperties.getAccessExpire(),
                TimeUnit.MILLISECONDS);

        // 构建响应
        LoginResponse response = LoginResponse.from(user, newToken, (long) jwtProperties.getAccessExpire());

        return Result.success("刷新成功", response);
    }

    /**
     * 生成用户编号
     */
    private String generateUserNo() {
        return "U" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
