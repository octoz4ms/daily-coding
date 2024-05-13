package com.octo.ssd.controller;
import com.octo.ssd.entity.User;
import com.octo.ssd.security.JwtUtils;
import com.octo.ssd.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public Map<String,Object> login(@RequestBody User user) {
        HashMap<String, Object> result = new HashMap<>();
        // 用户认证
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        // 认证失败
        if(Objects.isNull(authenticate)) {
            return result;
        }
        // 生成token
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        String token = JwtUtils.generateToken(loginUser.getUsername());
        result.put("token", token);
        result.put("user", loginUser);

        //todo  存储token -> redis
        return result;
    }
}
