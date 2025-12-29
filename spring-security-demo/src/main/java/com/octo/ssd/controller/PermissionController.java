package com.octo.ssd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.octo.ssd.common.Result;
import com.octo.ssd.dto.PermissionDTO;
import com.octo.ssd.entity.Permission;
import com.octo.ssd.service.IPermissionService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 *
 * @author octo
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Resource
    private IPermissionService permissionService;

    /**
     * 分页查询权限列表
     *
     * @param page     当前页（默认1）
     * @param size     每页大小（默认10）
     * @param permName 权限名称（模糊查询）
     * @return 权限分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:list')")
    public Result<IPage<Permission>> getPermissionPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String permName) {
        IPage<Permission> permissionPage = permissionService.getPermissionPage(page, size, permName);
        return Result.success(permissionPage);
    }

    /**
     * 获取所有权限列表（不分页，用于下拉选择）
     *
     * @return 权限列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:list')")
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }

    /**
     * 获取权限详情
     *
     * @param id 权限ID
     * @return 权限详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:query')")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        Permission permission = permissionService.getPermissionDetail(id);
        return Result.success(permission);
    }

    /**
     * 创建权限
     *
     * @param permissionDTO 权限信息
     * @return 创建的权限
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:add')")
    public Result<Permission> createPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        Permission permission = permissionService.createPermission(permissionDTO);
        return Result.success("权限创建成功", permission);
    }

    /**
     * 更新权限
     *
     * @param id            权限ID
     * @param permissionDTO 权限信息
     * @return 更新后的权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:edit')")
    public Result<Permission> updatePermission(@PathVariable Long id, @Valid @RequestBody PermissionDTO permissionDTO) {
        permissionDTO.setId(id);
        Permission permission = permissionService.updatePermission(permissionDTO);
        return Result.success("权限更新成功", permission);
    }

    /**
     * 删除权限
     *
     * @param id 权限ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('permission:delete')")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success("权限删除成功", null);
    }
}
