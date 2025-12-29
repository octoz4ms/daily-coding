package com.octo.ssd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.ssd.common.ResultCode;
import com.octo.ssd.dto.RoleDTO;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.entity.Role;
import com.octo.ssd.entity.RolePermission;
import com.octo.ssd.entity.UserRole;
import com.octo.ssd.exception.BusinessException;
import com.octo.ssd.mapper.PermissionMapper;
import com.octo.ssd.mapper.RoleMapper;
import com.octo.ssd.service.IRolePermissionService;
import com.octo.ssd.service.IRoleService;
import com.octo.ssd.service.IUserRoleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 *
 * @author octo
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private IRolePermissionService rolePermissionService;

    @Resource
    private IUserRoleService userRoleService;

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public IPage<Role> getRolePage(int page, int size, String roleName) {
        Page<Role> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Role> queryWrapper = Wrappers.lambdaQuery(Role.class);

        if (StringUtils.hasText(roleName)) {
            queryWrapper.like(Role::getRoleName, roleName);
        }

        queryWrapper.orderByDesc(Role::getCreateTime);

        return this.page(pageParam, queryWrapper);
    }

    @Override
    public List<Role> getAllRoles() {
        return this.list(Wrappers.lambdaQuery(Role.class)
                .orderByDesc(Role::getCreateTime));
    }

    @Override
    public Role getRoleDetail(Long roleId) {
        Role role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role createRole(RoleDTO roleDTO) {
        // 检查角色编码是否已存在
        if (getByRoleCode(roleDTO.getRoleCode()) != null) {
            throw new BusinessException(ResultCode.ROLE_ALREADY_EXISTS);
        }

        // 创建角色
        Role role = new Role();
        role.setRoleNo(generateRoleNo());
        role.setRoleCode(roleDTO.getRoleCode());
        role.setRoleName(roleDTO.getRoleName());
        role.setDescription(roleDTO.getDescription());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());

        this.save(role);

        // 分配权限
        if (!CollectionUtils.isEmpty(roleDTO.getPermissionIds())) {
            assignPermissions(role.getId(), roleDTO.getPermissionIds());
        }

        return getRoleDetail(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role updateRole(RoleDTO roleDTO) {
        if (roleDTO.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色ID不能为空");
        }

        Role existingRole = this.getById(roleDTO.getId());
        if (existingRole == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 更新角色基本信息
        boolean needUpdate = false;
        if (StringUtils.hasText(roleDTO.getRoleCode())) {
            // 检查角色编码是否被其他角色使用
            Role roleByCode = getByRoleCode(roleDTO.getRoleCode());
            if (roleByCode != null && !roleByCode.getId().equals(roleDTO.getId())) {
                throw new BusinessException(ResultCode.ROLE_ALREADY_EXISTS);
            }
            existingRole.setRoleCode(roleDTO.getRoleCode());
            needUpdate = true;
        }

        if (StringUtils.hasText(roleDTO.getRoleName())) {
            existingRole.setRoleName(roleDTO.getRoleName());
            needUpdate = true;
        }

        if (roleDTO.getDescription() != null) {
            existingRole.setDescription(roleDTO.getDescription());
            needUpdate = true;
        }

        if (needUpdate) {
            existingRole.setUpdateTime(LocalDateTime.now());
            this.updateById(existingRole);
        }

        // 更新权限
        if (roleDTO.getPermissionIds() != null) {
            // 先删除原有权限
            rolePermissionService.remove(Wrappers.lambdaQuery(RolePermission.class)
                    .eq(RolePermission::getRoleId, roleDTO.getId()));
            // 分配新权限
            if (!CollectionUtils.isEmpty(roleDTO.getPermissionIds())) {
                assignPermissions(roleDTO.getId(), roleDTO.getPermissionIds());
            }
        }

        return getRoleDetail(roleDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Role role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 检查是否有用户关联该角色
        long userCount = userRoleService.count(Wrappers.lambdaQuery(UserRole.class)
                .eq(UserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该角色下存在用户，无法删除");
        }

        // 删除角色权限关联
        rolePermissionService.remove(Wrappers.lambdaQuery(RolePermission.class)
                .eq(RolePermission::getRoleId, roleId));

        // 删除角色
        this.removeById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }

        List<RolePermission> rolePermissions = permissionIds.stream()
                .map(permissionId -> {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRoleId(roleId);
                    rolePermission.setPermId(permissionId);
                    return rolePermission;
                })
                .collect(Collectors.toList());

        rolePermissionService.saveBatch(rolePermissions);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public Role getByRoleCode(String roleCode) {
        return this.getOne(Wrappers.lambdaQuery(Role.class)
                .eq(Role::getRoleCode, roleCode));
    }

    /**
     * 生成角色编号
     */
    private String generateRoleNo() {
        return "R" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
