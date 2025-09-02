package com.octo.ssd.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.octo.ssd.entity.User;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private IUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class).eq(User::getUsername, username);
        User user = userService.getOne(queryWrapper);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        // Mock 角色和权限
        user.setRoles(List.of("ADMIN", "USER"));
        user.setPermissions(List.of("system:role", "system:user"));
        Collection<SimpleGrantedAuthority> authorities = createAuthorities(user.getRoles(), user.getPermissions());
        return new LoginUser(user, authorities);
    }

    public static Collection<SimpleGrantedAuthority> createAuthorities(
            List<String> roles, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        authorities.addAll(
                permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        return authorities;
    }
}
