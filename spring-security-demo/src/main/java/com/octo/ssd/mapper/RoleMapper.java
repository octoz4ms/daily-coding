package com.octo.ssd.mapper;

import com.octo.ssd.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-09-09
 */
public interface RoleMapper extends BaseMapper<Role> {
    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> selectRolesByUserId(Long userId);
}
