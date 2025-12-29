package com.octo.eum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.eum.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 *
 * @author octo
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据用户ID查询角色列表
     */
    List<Role> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询角色（包含权限）
     */
    Role selectRoleWithPermissions(@Param("roleId") Long roleId);

    /**
     * 检查角色编码是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_role WHERE code = #{code} AND deleted = 0 AND id != #{excludeId}")
    int countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 检查角色是否被用户使用
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUserByRoleId(@Param("roleId") Long roleId);
}

