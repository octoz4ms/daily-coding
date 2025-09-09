package com.octo.ssd.service.impl;

import com.octo.ssd.entity.Role;
import com.octo.ssd.mapper.RoleMapper;
import com.octo.ssd.service.IRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
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
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {
    @Resource
    private RoleMapper roleMapper;

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }
}
