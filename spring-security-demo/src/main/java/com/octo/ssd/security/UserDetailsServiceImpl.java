package com.octo.ssd.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.entity.Role;
import com.octo.ssd.entity.User;
import com.octo.ssd.service.IPermissionService;
import com.octo.ssd.service.IRoleService;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private IUserService userService;

    @Resource
    private IRoleService roleService;

    @Resource
    private IPermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, username);
        User user = userService.getOne(queryWrapper);
        if (user == null) {
            throw new UsernameNotFoundException("该用户不存在！");
        }

        // 查询用户角色
        List<Role> roles = roleService.getRolesByUserId(user.getId());
        user.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));

        // 查询用户权限
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
        user.setPermissions(permissions.stream().map(Permission::getPermCode).collect(Collectors.toList()));

        return new LoginUser(user);
    }
}
