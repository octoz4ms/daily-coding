package com.octo.eum.controller;

import com.octo.eum.common.PageResult;
import com.octo.eum.common.Result;
import com.octo.eum.dto.request.PageRequest;
import com.octo.eum.dto.request.RoleCreateRequest;
import com.octo.eum.dto.request.RoleUpdateRequest;
import com.octo.eum.dto.response.RoleVO;
import com.octo.eum.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author octo
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public Result<Long> createRole(@Valid @RequestBody RoleCreateRequest request) {
        Long roleId = roleService.createRole(request);
        return Result.success("角色创建成功", roleId);
    }

    /**
     * 更新角色
     */
    @PutMapping
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Void> updateRole(@Valid @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(request);
        return Result.success("角色更新成功", null);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("角色删除成功", null);
    }

    /**
     * 批量删除角色
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> batchDeleteRoles(@RequestBody List<Long> ids) {
        roleService.batchDeleteRoles(ids);
        return Result.success("批量删除成功", null);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 分页查询角色
     */
    @GetMapping
    @PreAuthorize("hasAuthority('role:view')")
    public Result<PageResult<RoleVO>> pageRoles(PageRequest request,
                                                 @RequestParam(required = false) String keyword) {
        PageResult<RoleVO> result = roleService.pageRoles(request, keyword);
        return Result.success(result);
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('role:view')")
    public Result<List<RoleVO>> listAllRoles() {
        List<RoleVO> roles = roleService.listAllRoles();
        return Result.success(roles);
    }

    /**
     * 分配权限
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign-permission')")
    public Result<Void> assignPermissions(@PathVariable Long id,
                                           @RequestBody List<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success("权限分配成功", null);
    }
}

