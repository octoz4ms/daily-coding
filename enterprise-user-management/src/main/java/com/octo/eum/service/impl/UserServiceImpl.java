package com.octo.eum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.eum.common.PageResult;
import com.octo.eum.common.ResultCode;
import com.octo.eum.config.RabbitMQConfig;
import com.octo.eum.dto.request.PasswordUpdateRequest;
import com.octo.eum.dto.request.UserCreateRequest;
import com.octo.eum.dto.request.UserQueryRequest;
import com.octo.eum.dto.request.UserUpdateRequest;
import com.octo.eum.dto.response.RoleVO;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.Role;
import com.octo.eum.entity.User;
import com.octo.eum.entity.UserRole;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.mapper.RoleMapper;
import com.octo.eum.mapper.UserMapper;
import com.octo.eum.mapper.UserRoleMapper;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateRequest request) {
        // 检查用户名是否存在
        if (existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 创建用户
        User user = new User();
        BeanUtil.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        user.setLoginFailCount(0);
        save(user);

        // 分配角色
        if (CollUtil.isNotEmpty(request.getRoleIds())) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        // 发送用户创建事件
        rabbitTemplate.convertAndSend(RabbitMQConfig.USER_EVENT_EXCHANGE,
                RabbitMQConfig.USER_CREATE_ROUTING_KEY, user);

        log.info("用户创建成功: {}", user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#request.id")
    public void updateUser(UserUpdateRequest request) {
        // 检查用户是否存在
        User user = getById(request.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 更新用户信息
        BeanUtil.copyProperties(request, user, "id", "password");
        updateById(user);

        // 更新角色
        if (request.getRoleIds() != null) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        log.info("用户更新成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 删除用户角色关联
        userRoleMapper.deleteByUserId(id);

        // 逻辑删除用户
        removeById(id);

        log.info("用户删除成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        ids.forEach(this::deleteUser);
    }

    @Override
    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    public UserVO getUserById(Long id) {
        User user = userMapper.selectUserWithRoles(id);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserByUsername(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getUsername()), User::getUsername, request.getUsername())
                .like(StrUtil.isNotBlank(request.getNickname()), User::getNickname, request.getNickname())
                .like(StrUtil.isNotBlank(request.getPhone()), User::getPhone, request.getPhone())
                .like(StrUtil.isNotBlank(request.getEmail()), User::getEmail, request.getEmail())
                .eq(request.getStatus() != null, User::getStatus, request.getStatus())
                .ge(request.getCreateTimeStart() != null, User::getCreateTime, request.getCreateTimeStart())
                .le(request.getCreateTimeEnd() != null, User::getCreateTime, request.getCreateTimeEnd())
                .orderByDesc(User::getCreateTime);

        // 分页查询
        IPage<User> page = new Page<>(request.getPageNum(), request.getPageSize());
        page = page(page, wrapper);

        // 转换为VO并填充角色信息
        List<UserVO> voList = page.getRecords().stream()
                .map(user -> {
                    UserVO vo = convertToVO(user);
                    // 查询用户角色
                    List<Role> roles = roleMapper.selectByUserId(user.getId());
                    vo.setRoles(roles.stream().map(this::convertRoleToVO).collect(Collectors.toList()));
                    vo.setRoleIds(roles.stream().map(Role::getId).collect(Collectors.toList()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<UserVO> result = new PageResult<>();
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(voList);
        result.setHasPrevious(page.getCurrent() > 1);
        result.setHasNext(page.getCurrent() < page.getPages());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordUpdateRequest request) {
        // 验证确认密码
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "两次输入的密码不一致");
        }

        // 获取当前用户
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void resetPassword(Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLoginFailCount(0);
        user.setLockTime(null);
        updateById(user);

        log.info("管理员重置用户密码: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user", key = "#userId")
    public void updateStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setStatus(status);
        if (status == 1) {
            // 启用时清除锁定信息
            user.setLoginFailCount(0);
            user.setLockTime(null);
        }
        updateById(user);

        log.info("更新用户状态: {} -> {}", user.getUsername(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色关联
        userRoleMapper.deleteByUserId(userId);

        // 添加新角色关联
        if (CollUtil.isNotEmpty(roleIds)) {
            List<UserRole> userRoles = roleIds.stream()
                    .map(roleId -> new UserRole(userId, roleId))
                    .collect(Collectors.toList());
            userRoleMapper.batchInsert(userRoles);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userMapper.countByUsername(username) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLoginInfo(Long userId, String ip) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        user.setLoginFailCount(0);
        updateById(user);
    }

    /**
     * 转换为VO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);

        // 填充角色信息
        if (CollUtil.isNotEmpty(user.getRoles())) {
            vo.setRoles(user.getRoles().stream()
                    .map(this::convertRoleToVO)
                    .collect(Collectors.toList()));
            vo.setRoleIds(user.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * 转换角色为VO
     */
    private RoleVO convertRoleToVO(Role role) {
        RoleVO vo = new RoleVO();
        BeanUtil.copyProperties(role, vo);
        return vo;
    }
}

