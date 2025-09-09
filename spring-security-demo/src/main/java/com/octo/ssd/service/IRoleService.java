package com.octo.ssd.service;

import com.octo.ssd.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2025-09-09
 */
public interface IRoleService extends IService<Role> {
    /**
     * 根据用户ID获取角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);
}
