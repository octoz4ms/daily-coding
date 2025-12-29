package com.octo.eum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.eum.dto.response.PermissionVO;
import com.octo.eum.entity.Permission;

import java.util.List;

/**
 * 权限服务接口
 *
 * @author octo
 */
public interface PermissionService extends IService<Permission> {

    /**
     * 创建权限
     *
     * @param permission 权限实体
     * @return 权限ID
     */
    Long createPermission(Permission permission);

    /**
     * 更新权限
     *
     * @param permission 权限实体
     */
    void updatePermission(Permission permission);

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    void deletePermission(Long id);

    /**
     * 根据ID获取权限
     *
     * @param id 权限ID
     * @return 权限VO
     */
    PermissionVO getPermissionById(Long id);

    /**
     * 获取权限树
     *
     * @return 权限树
     */
    List<PermissionVO> getPermissionTree();

    /**
     * 获取所有权限列表（扁平）
     *
     * @return 权限列表
     */
    List<PermissionVO> listAllPermissions();

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<PermissionVO> getPermissionsByRoleId(Long roleId);

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<PermissionVO> getPermissionsByUserId(Long userId);

    /**
     * 根据用户ID获取菜单树
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<PermissionVO> getMenuTreeByUserId(Long userId);

    /**
     * 检查权限编码是否存在
     *
     * @param code      权限编码
     * @param excludeId 排除的权限ID
     * @return 是否存在
     */
    boolean existsByCode(String code, Long excludeId);
}

