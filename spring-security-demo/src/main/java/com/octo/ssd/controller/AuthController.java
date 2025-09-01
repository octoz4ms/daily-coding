package com.octo.ssd.controller;

import com.octo.ssd.entity.User;
import com.octo.ssd.security.JwtUtils;
import com.octo.ssd.security.LoginUser;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthenticationManager authenticationManager;

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

        // todo  存储token -> redis

        // 构建返回结果
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("token", token);
        resultMap.put("user", loginUser.getUser()); // 只返回User实体，避免暴露UserDetails细节

        return resultMap;
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String token) {
        if (JwtUtils.validateToken(token)) {
            // todo  从redis中删除token
            return "注销成功";
        } else {
            return "注销失败";
        }

    }
}
