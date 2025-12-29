package com.octo.ssd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.ssd.dto.PermissionDTO;
import com.octo.ssd.entity.Permission;

import java.util.List;

/**
 * 权限服务接口
 *
 * @author octo
 */
public interface IPermissionService extends IService<Permission> {

    /**
     * 根据用户ID查询权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);

    /**
     * 分页查询权限列表
     *
     * @param page     当前页
     * @param size     每页大小
     * @param permName 权限名称（模糊查询）
     * @return 权限分页列表
     */
    IPage<Permission> getPermissionPage(int page, int size, String permName);

    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据ID获取权限详情
     *
     * @param permissionId 权限ID
     * @return 权限详情
     */
    Permission getPermissionDetail(Long permissionId);

    /**
     * 创建权限
     *
     * @param permissionDTO 权限信息
     * @return 创建的权限
     */
    Permission createPermission(PermissionDTO permissionDTO);

    /**
     * 更新权限
     *
     * @param permissionDTO 权限信息
     * @return 更新后的权限
     */
    Permission updatePermission(PermissionDTO permissionDTO);

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     */
    void deletePermission(Long permissionId);

    /**
     * 根据权限编码查询权限
     *
     * @param permCode 权限编码
     * @return 权限信息
     */
    Permission getByPermCode(String permCode);
}
