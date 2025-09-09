package com.octo.ssd.service;

import com.octo.ssd.entity.Permission;
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
public interface IPermissionService extends IService<Permission> {
    /**
     * 根据用户ID查询权限列表
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);
}

