package com.octo.ssd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.ssd.entity.Permission;

import java.util.List;

/**
 * 权限Mapper接口
 *
 * @author octo
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> selectPermissionsByUserId(Long userId);

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> selectPermissionsByRoleId(Long roleId);
}
