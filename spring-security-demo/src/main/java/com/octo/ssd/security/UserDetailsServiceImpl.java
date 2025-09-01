package com.octo.ssd.security;

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
        return userService.lambdaQuery()
                .eq(User::getUsername, username)
                .oneOpt().map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }
}
