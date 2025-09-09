package com.octo.ssd.mapper;

import com.octo.ssd.entity.Permission;
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
public interface PermissionMapper extends BaseMapper<Permission> {

    List<Permission> selectPermissionsByUserId(Long userId);
}
