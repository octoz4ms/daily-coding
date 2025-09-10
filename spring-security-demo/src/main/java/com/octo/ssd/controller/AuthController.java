package com.octo.ssd.controller;

import com.octo.ssd.config.JwtProperties;
import com.octo.ssd.entity.User;
import com.octo.ssd.security.JwtUtils;
import com.octo.ssd.security.LoginUser;
import com.octo.ssd.utils.RedisUtil;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private JwtProperties jwtProperties;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user) {
        // 使用AuthenticationManager进行身份验证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );

        // 验证通过后，从Authentication中获取登录用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 生成JWT token
        String token = JwtUtils.generateToken(loginUser.getUsername());

        // 存储token -> redis
        redisUtil.set("login:user:" + token,
                loginUser.getUser(),
                jwtProperties.getAccessExpire(),
                TimeUnit.MILLISECONDS);

        // 构建返回结果
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("token", token);
        resultMap.put("user", loginUser.getUser()); // 只返回User实体，避免暴露UserDetails细节

        return resultMap;
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            return "注销成功";
        }

        String jwtToken = token.substring(7);
        redisUtil.delete("login:user:" + jwtToken);
        return "注销成功";
    }
}
