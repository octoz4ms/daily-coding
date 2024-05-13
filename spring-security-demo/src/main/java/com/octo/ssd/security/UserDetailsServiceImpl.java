package com.octo.ssd.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.octo.ssd.entity.User;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户信息
        User user = userService.getOne(Wrappers.lambdaQuery(User.class).eq(User::getUsername, username));
        // 封装数据 -> UserDetails
        if (user == null) {
            return null;
        }
        return new LoginUser(user);
    }
}
