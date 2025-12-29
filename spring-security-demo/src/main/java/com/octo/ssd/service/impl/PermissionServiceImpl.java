package com.octo.ssd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.ssd.common.ResultCode;
import com.octo.ssd.dto.PermissionDTO;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.entity.RolePermission;
import com.octo.ssd.exception.BusinessException;
import com.octo.ssd.mapper.PermissionMapper;
import com.octo.ssd.service.IPermissionService;
import com.octo.ssd.service.IRolePermissionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 权限服务实现类
 *
 * @author octo
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private IRolePermissionService rolePermissionService;

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public IPage<Permission> getPermissionPage(int page, int size, String permName) {
        Page<Permission> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Permission> queryWrapper = Wrappers.lambdaQuery(Permission.class);

        if (StringUtils.hasText(permName)) {
            queryWrapper.like(Permission::getPermName, permName);
        }

        queryWrapper.orderByDesc(Permission::getCreateTime);

        return this.page(pageParam, queryWrapper);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return this.list(Wrappers.lambdaQuery(Permission.class)
                .orderByAsc(Permission::getPermCode));
    }

    @Override
    public Permission getPermissionDetail(Long permissionId) {
        Permission permission = this.getById(permissionId);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }
        return permission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Permission createPermission(PermissionDTO permissionDTO) {
        // 检查权限编码是否已存在
        if (getByPermCode(permissionDTO.getPermCode()) != null) {
            throw new BusinessException(ResultCode.PERMISSION_ALREADY_EXISTS);
        }

        // 创建权限
        Permission permission = new Permission();
        permission.setPermNo(generatePermNo());
        permission.setPermCode(permissionDTO.getPermCode());
        permission.setPermName(permissionDTO.getPermName());
        permission.setDescription(permissionDTO.getDescription());
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());

        this.save(permission);

        return getPermissionDetail(permission.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Permission updatePermission(PermissionDTO permissionDTO) {
        if (permissionDTO.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "权限ID不能为空");
        }

        Permission existingPermission = this.getById(permissionDTO.getId());
        if (existingPermission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 检查权限编码是否被其他权限使用
        Permission permByCode = getByPermCode(permissionDTO.getPermCode());
        if (permByCode != null && !permByCode.getId().equals(permissionDTO.getId())) {
            throw new BusinessException(ResultCode.PERMISSION_ALREADY_EXISTS);
        }

        // 更新权限信息
        existingPermission.setPermCode(permissionDTO.getPermCode());
        existingPermission.setPermName(permissionDTO.getPermName());
        existingPermission.setDescription(permissionDTO.getDescription());
        existingPermission.setUpdateTime(LocalDateTime.now());

        this.updateById(existingPermission);

        return getPermissionDetail(permissionDTO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long permissionId) {
        Permission permission = this.getById(permissionId);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 检查是否有角色关联该权限
        long roleCount = rolePermissionService.count(Wrappers.lambdaQuery(RolePermission.class)
                .eq(RolePermission::getPermId, permissionId));
        if (roleCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该权限已被角色使用，无法删除");
        }

        // 删除权限
        this.removeById(permissionId);
    }

    @Override
    public Permission getByPermCode(String permCode) {
        return this.getOne(Wrappers.lambdaQuery(Permission.class)
                .eq(Permission::getPermCode, permCode));
    }

    /**
     * 生成权限编号
     */
    private String generatePermNo() {
        return "P" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
