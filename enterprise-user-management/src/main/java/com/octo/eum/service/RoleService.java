package com.octo.eum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.eum.common.PageResult;
import com.octo.eum.dto.request.PageRequest;
import com.octo.eum.dto.request.RoleCreateRequest;
import com.octo.eum.dto.request.RoleUpdateRequest;
import com.octo.eum.dto.response.RoleVO;
import com.octo.eum.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author octo
 */
public interface RoleService extends IService<Role> {

    /**
     * 创建角色
     *
     * @param request 创建请求
     * @return 角色ID
     */
    Long createRole(RoleCreateRequest request);

    /**
     * 更新角色
     *
     * @param request 更新请求
     */
    void updateRole(RoleUpdateRequest request);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID列表
     */
    void batchDeleteRoles(List<Long> ids);

    /**
     * 根据ID获取角色
     *
     * @param id 角色ID
     * @return 角色VO
     */
    RoleVO getRoleById(Long id);

    /**
     * 分页查询角色
     *
     * @param request 分页请求
     * @param keyword 搜索关键字
     * @return 分页结果
     */
    PageResult<RoleVO> pageRoles(PageRequest request, String keyword);

    /**
     * 获取所有角色列表
     *
     * @return 角色列表
     */
    List<RoleVO> listAllRoles();

    /**
     * 分配权限
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 检查角色编码是否存在
     *
     * @param code      角色编码
     * @param excludeId 排除的角色ID
     * @return 是否存在
     */
    boolean existsByCode(String code, Long excludeId);
}

