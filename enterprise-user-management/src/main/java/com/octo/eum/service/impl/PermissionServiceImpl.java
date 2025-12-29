package com.octo.eum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.octo.eum.common.ResultCode;
import com.octo.eum.dto.response.PermissionVO;
import com.octo.eum.entity.Permission;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.mapper.PermissionMapper;
import com.octo.eum.mapper.RolePermissionMapper;
import com.octo.eum.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permission", allEntries = true)
    public Long createPermission(Permission permission) {
        // 检查权限编码是否存在
        if (existsByCode(permission.getCode(), null)) {
            throw new BusinessException(ResultCode.PERMISSION_ALREADY_EXISTS);
        }

        save(permission);
        log.info("权限创建成功: {}", permission.getName());
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permission", allEntries = true)
    public void updatePermission(Permission permission) {
        Permission existing = getById(permission.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 检查权限编码是否冲突
        if (!existing.getCode().equals(permission.getCode())
                && existsByCode(permission.getCode(), permission.getId())) {
            throw new BusinessException(ResultCode.PERMISSION_ALREADY_EXISTS);
        }

        updateById(permission);
        log.info("权限更新成功: {}", permission.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "permission", allEntries = true)
    public void deletePermission(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_FOUND);
        }

        // 检查是否有子权限
        if (permissionMapper.countChildren(id) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子权限，无法删除");
        }

        // 检查是否被角色使用
        if (permissionMapper.countRoleByPermissionId(id) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "权限正在被使用，无法删除");
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(id);

        // 逻辑删除权限
        removeById(id);

        log.info("权限删除成功: {}", permission.getName());
    }

    @Override
    @Cacheable(value = "permission", key = "#id", unless = "#result == null")
    public PermissionVO getPermissionById(Long id) {
        Permission permission = getById(id);
        if (permission == null) {
            return null;
        }
        return convertToVO(permission);
    }

    @Override
    @Cacheable(value = "permission", key = "'tree'")
    public List<PermissionVO> getPermissionTree() {
        // 查询所有权限
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort);
        List<Permission> permissions = list(wrapper);

        // 转换为VO
        List<PermissionVO> voList = permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(voList);
    }

    @Override
    public List<PermissionVO> listAllPermissions() {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getStatus, 1)
                .orderByAsc(Permission::getSort);
        List<Permission> permissions = list(wrapper);
        return permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionVO> getPermissionsByRoleId(Long roleId) {
        List<Permission> permissions = permissionMapper.selectByRoleId(roleId);
        return permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionVO> getPermissionsByUserId(Long userId) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionVO> getMenuTreeByUserId(Long userId) {
        // 查询用户权限（只包含目录和菜单）
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        List<PermissionVO> voList = permissions.stream()
                .filter(p -> p.getType() != null && p.getType() <= 2) // 只保留目录和菜单
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(voList);
    }

    @Override
    public boolean existsByCode(String code, Long excludeId) {
        return permissionMapper.countByCode(code, excludeId != null ? excludeId : 0L) > 0;
    }

    /**
     * 构建树形结构
     */
    private List<PermissionVO> buildTree(List<PermissionVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return new ArrayList<>();
        }

        // 按父ID分组
        Map<Long, List<PermissionVO>> parentIdMap = voList.stream()
                .collect(Collectors.groupingBy(vo ->
                        vo.getParentId() != null ? vo.getParentId() : 0L));

        // 设置子节点
        voList.forEach(vo -> {
            List<PermissionVO> children = parentIdMap.get(vo.getId());
            if (CollUtil.isNotEmpty(children)) {
                vo.setChildren(children);
            }
        });

        // 返回根节点
        return voList.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private PermissionVO convertToVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        BeanUtil.copyProperties(permission, vo);
        return vo;
    }
}

