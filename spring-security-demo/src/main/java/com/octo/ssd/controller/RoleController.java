package com.octo.ssd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.octo.ssd.common.Result;
import com.octo.ssd.dto.RoleDTO;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.entity.Role;
import com.octo.ssd.service.IRoleService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author octo
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    @Resource
    private IRoleService roleService;

    /**
     * 分页查询角色列表
     *
     * @param page     当前页（默认1）
     * @param size     每页大小（默认10）
     * @param roleName 角色名称（模糊查询）
     * @return 角色分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:list')")
    public Result<IPage<Role>> getRolePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName) {
        IPage<Role> rolePage = roleService.getRolePage(page, size, roleName);
        return Result.success(rolePage);
    }

    /**
     * 获取所有角色列表（不分页，用于下拉选择）
     *
     * @return 角色列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:list')")
    public Result<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    /**
     * 获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:query')")
    public Result<Role> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleDetail(id);
        return Result.success(role);
    }

    /**
     * 创建角色
     *
     * @param roleDTO 角色信息
     * @return 创建的角色
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:add')")
    public Result<Role> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        Role role = roleService.createRole(roleDTO);
        return Result.success("角色创建成功", role);
    }

    /**
     * 更新角色
     *
     * @param id      角色ID
     * @param roleDTO 角色信息
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:edit')")
    public Result<Role> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        Role role = roleService.updateRole(roleDTO);
        return Result.success("角色更新成功", role);
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success("角色删除成功", null);
    }

    /**
     * 为角色分配权限
     *
     * @param id            角色ID
     * @param permissionIds 权限ID列表
     * @return 分配结果
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:assignPermission')")
    public Result<Role> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId(id);
        roleDTO.setPermissionIds(permissionIds);
        Role role = roleService.updateRole(roleDTO);
        return Result.success("权限分配成功", role);
    }

    /**
     * 获取角色的权限列表
     *
     * @param id 角色ID
     * @return 权限列表
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('role:query')")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long id) {
        List<Permission> permissions = roleService.getPermissionsByRoleId(id);
        return Result.success(permissions);
    }
}
