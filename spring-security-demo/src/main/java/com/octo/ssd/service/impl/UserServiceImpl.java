package com.octo.ssd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.ssd.common.ResultCode;
import com.octo.ssd.dto.UserDTO;
import com.octo.ssd.entity.Role;
import com.octo.ssd.entity.User;
import com.octo.ssd.entity.UserRole;
import com.octo.ssd.exception.BusinessException;
import com.octo.ssd.mapper.UserMapper;
import com.octo.ssd.service.IRoleService;
import com.octo.ssd.service.IUserRoleService;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author octo
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private IUserRoleService userRoleService;

    @Resource
    private IRoleService roleService;

    @Override
    public IPage<User> getUserPage(int page, int size, String username) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class);

        if (StringUtils.hasText(username)) {
            queryWrapper.like(User::getUsername, username);
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        IPage<User> userPage = this.page(pageParam, queryWrapper);

        // 查询每个用户的角色
        userPage.getRecords().forEach(user -> {
            List<Role> roles = roleService.getRolesByUserId(user.getId());
            user.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
            user.setPassword(null); // 不返回密码
        });

        return userPage;
    }

    @Override
    public User getUserDetail(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 查询用户角色
        List<Role> roles = roleService.getRolesByUserId(userId);
        user.setRoles(roles.stream().map(Role::getRoleCode).collect(Collectors.toList()));
        user.setPassword(null); // 不返回密码

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(UserDTO userDTO) {
        // 检查用户名是否已存在
        if (getByUsername(userDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setUserNo(generateUserNo());
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        this.save(user);

        // 分配角色
        if (!CollectionUtils.isEmpty(userDTO.getRoleIds())) {
            assignRoles(user.getId(), userDTO.getRoleIds());
        }

        return getUserDetail(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(UserDTO userDTO) {
        if (userDTO.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID不能为空");
        }

        User existingUser = this.getById(userDTO.getId());
        if (existingUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 更新用户基本信息
        boolean needUpdate = false;
        if (StringUtils.hasText(userDTO.getUsername())) {
            // 检查用户名是否被其他用户使用
            User userByUsername = getByUsername(userDTO.getUsername());
            if (userByUsername != null && !userByUsername.getId().equals(userDTO.getId())) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
            }
            existingUser.setUsername(userDTO.getUsername());
            needUpdate = true;
        }

        if (StringUtils.hasText(userDTO.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            needUpdate = true;
        }

        if (needUpdate) {
            existingUser.setUpdateTime(LocalDateTime.now());
            this.updateById(existingUser);
        }

        // 更新角色
        if (userDTO.getRoleIds() != null) {
            // 先删除原有角色
            userRoleService.remove(Wrappers.lambdaQuery(UserRole.class)
                    .eq(UserRole::getUserId, userDTO.getId()));
            // 分配新角色
            if (!CollectionUtils.isEmpty(userDTO.getRoleIds())) {
                assignRoles(userDTO.getId(), userDTO.getRoleIds());
            }
        }

        return getUserDetail(userDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 删除用户角色关联
        userRoleService.remove(Wrappers.lambdaQuery(UserRole.class)
                .eq(UserRole::getUserId, userId));

        // 删除用户
        this.removeById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }

        List<UserRole> userRoles = roleIds.stream()
                .map(roleId -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                })
                .collect(Collectors.toList());

        userRoleService.saveBatch(userRoles);
    }

    @Override
    public User getByUsername(String username) {
        return this.getOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, username));
    }

    /**
     * 生成用户编号
     */
    private String generateUserNo() {
        return "U" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
