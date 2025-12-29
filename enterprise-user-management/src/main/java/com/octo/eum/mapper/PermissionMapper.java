package com.octo.eum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.eum.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 *
 * @author octo
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据角色ID查询权限列表
     */
    List<Permission> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限列表
     */
    List<Permission> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID列表查询权限列表
     */
    List<Permission> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 查询所有菜单权限（树形结构）
     */
    @Select("SELECT * FROM sys_permission WHERE deleted = 0 AND status = 1 ORDER BY sort ASC")
    List<Permission> selectAllMenus();

    /**
     * 检查权限编码是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_permission WHERE code = #{code} AND deleted = 0 AND id != #{excludeId}")
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 检查权限是否被角色使用
     */
    @Select("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = #{permissionId}")
    int countRoleByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 查询子权限数量
     */
    @Select("SELECT COUNT(*) FROM sys_permission WHERE parent_id = #{parentId} AND deleted = 0")
    int countChildren(@Param("parentId") Long parentId);
}

