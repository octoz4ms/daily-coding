package com.octo.ssd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.ssd.dto.RoleDTO;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author octo
 */
public interface IRoleService extends IService<Role> {

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 分页查询角色列表
     *
     * @param page     当前页
     * @param size     每页大小
     * @param roleName 角色名称（模糊查询）
     * @return 角色分页列表
     */
    IPage<Role> getRolePage(int page, int size, String roleName);

    /**
     * 获取所有角色列表
     *
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 根据ID获取角色详情（包含权限信息）
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    Role getRoleDetail(Long roleId);

    /**
     * 创建角色
     *
     * @param roleDTO 角色信息
     * @return 创建的角色
     */
    Role createRole(RoleDTO roleDTO);

    /**
     * 更新角色
     *
     * @param roleDTO 角色信息
     * @return 更新后的角色
     */
    Role updateRole(RoleDTO roleDTO);

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    void deleteRole(Long roleId);

    /**
     * 为角色分配权限
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role getByRoleCode(String roleCode);
}
