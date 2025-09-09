package com.octo.ssd.service.impl;

import com.octo.ssd.entity.Permission;
import com.octo.ssd.mapper.PermissionMapper;
import com.octo.ssd.service.IPermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2025-09-09
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Resource
    private PermissionMapper permissionMapper;

    /**
     * 根据用户ID查询权限列表
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }
}
