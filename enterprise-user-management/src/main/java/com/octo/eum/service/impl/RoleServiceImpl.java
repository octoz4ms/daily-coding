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
import com.octo.eum.dto.request.PageRequest;
import com.octo.eum.dto.request.RoleCreateRequest;
import com.octo.eum.dto.request.RoleUpdateRequest;
import com.octo.eum.dto.response.PermissionVO;
import com.octo.eum.dto.response.RoleVO;
import com.octo.eum.entity.Permission;
import com.octo.eum.entity.Role;
import com.octo.eum.entity.RolePermission;
import com.octo.eum.exception.BusinessException;
import com.octo.eum.mapper.PermissionMapper;
import com.octo.eum.mapper.RoleMapper;
import com.octo.eum.mapper.RolePermissionMapper;
import com.octo.eum.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateRequest request) {
        // 检查角色编码是否存在
        if (existsByCode(request.getCode(), null)) {
            throw new BusinessException(ResultCode.ROLE_ALREADY_EXISTS);
        }

        // 创建角色
        Role role = new Role();
        BeanUtil.copyProperties(request, role);
        save(role);

        // 分配权限
        if (CollUtil.isNotEmpty(request.getPermissionIds())) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        log.info("角色创建成功: {}", role.getName());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", key = "#request.id")
    public void updateRole(RoleUpdateRequest request) {
        // 检查角色是否存在
        Role role = getById(request.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 更新角色信息
        BeanUtil.copyProperties(request, role, "id", "code");
        updateById(role);

        // 更新权限
        if (request.getPermissionIds() != null) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        log.info("角色更新成功: {}", role.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", key = "#id")
    public void deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }

        // 检查角色是否被用户使用
        if (roleMapper.countUserByRoleId(id) > 0) {
            throw new BusinessException(ResultCode.ROLE_IN_USE);
        }

        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(id);

        // 逻辑删除角色
        removeById(id);

        log.info("角色删除成功: {}", role.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteRoles(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        ids.forEach(this::deleteRole);
    }

    @Override
    @Cacheable(value = "role", key = "#id", unless = "#result == null")
    public RoleVO getRoleById(Long id) {
        Role role = roleMapper.selectRoleWithPermissions(id);
        if (role == null) {
            return null;
        }
        return convertToVO(role);
    }

    @Override
    public PageResult<RoleVO> pageRoles(PageRequest request, String keyword) {
        // 构建查询条件
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StrUtil.isNotBlank(keyword), w ->
                w.like(Role::getCode, keyword)
                        .or()
                        .like(Role::getName, keyword)
        );
        wrapper.orderByAsc(Role::getSort)
                .orderByDesc(Role::getCreateTime);

        // 分页查询
        IPage<Role> page = new Page<>(request.getPageNum(), request.getPageSize());
        page = page(page, wrapper);

        // 转换为VO
        List<RoleVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建分页结果
        PageResult<RoleVO> result = new PageResult<>();
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
    public List<RoleVO> listAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1)
                .orderByAsc(Role::getSort);
        List<Role> roles = list(wrapper);
        return roles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "role", key = "#roleId")
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 添加新权限关联
        if (CollUtil.isNotEmpty(permissionIds)) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> new RolePermission(roleId, permissionId))
                    .collect(Collectors.toList());
            rolePermissionMapper.batchInsert(rolePermissions);
        }
    }

    @Override
    public boolean existsByCode(String code, Long excludeId) {
        return roleMapper.countByCode(code, excludeId != null ? excludeId : 0L) > 0;
    }

    /**
     * 转换为VO
     */
    private RoleVO convertToVO(Role role) {
        RoleVO vo = new RoleVO();
        BeanUtil.copyProperties(role, vo);

        // 填充权限信息
        if (CollUtil.isNotEmpty(role.getPermissions())) {
            vo.setPermissions(role.getPermissions().stream()
                    .map(this::convertPermissionToVO)
                    .collect(Collectors.toList()));
            vo.setPermissionIds(role.getPermissions().stream()
                    .map(Permission::getId)
                    .collect(Collectors.toList()));
        } else {
            // 查询权限ID列表
            List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(role.getId());
            vo.setPermissionIds(permissionIds);
        }

        return vo;
    }

    /**
     * 转换权限为VO
     */
    private PermissionVO convertPermissionToVO(Permission permission) {
        PermissionVO vo = new PermissionVO();
        BeanUtil.copyProperties(permission, vo);
        return vo;
    }
}

