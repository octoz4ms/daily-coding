package com.octo.eum.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.eum.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper
 *
 * @author octo
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（包含角色和权限）
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户（包含角色）
     */
    User selectUserWithRoles(@Param("userId") Long userId);

    /**
     * 查询用户的权限编码列表
     */
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的角色编码列表
     */
    @Select("SELECT r.code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE email = #{email} AND deleted = 0 AND id != #{excludeId}")
    int countByEmail(@Param("email") String email, @Param("excludeId") Long excludeId);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE phone = #{phone} AND deleted = 0 AND id != #{excludeId}")
    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);
}

