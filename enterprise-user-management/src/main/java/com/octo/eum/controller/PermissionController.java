package com.octo.eum.controller;

import com.octo.eum.common.Result;
import com.octo.eum.dto.response.PermissionVO;
import com.octo.eum.entity.Permission;
import com.octo.eum.security.SecurityUtils;
import com.octo.eum.service.PermissionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器
 *
 * @author octo
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public Result<Long> createPermission(@Valid @RequestBody Permission permission) {
        Long permissionId = permissionService.createPermission(permission);
        return Result.success("权限创建成功", permissionId);
    }

    /**
     * 更新权限
     */
    @PutMapping
    @PreAuthorize("hasAuthority('permission:update')")
    public Result<Void> updatePermission(@Valid @RequestBody Permission permission) {
        permissionService.updatePermission(permission);
        return Result.success("权限更新成功", null);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success("权限删除成功", null);
    }

    /**
     * 获取权限详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:view')")
    public Result<PermissionVO> getPermissionById(@PathVariable Long id) {
        PermissionVO permission = permissionService.getPermissionById(id);
        return Result.success(permission);
    }

    /**
     * 获取权限树
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('permission:view')")
    public Result<List<PermissionVO>> getPermissionTree() {
        List<PermissionVO> tree = permissionService.getPermissionTree();
        return Result.success(tree);
    }

    /**
     * 获取所有权限列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('permission:view')")
    public Result<List<PermissionVO>> listAllPermissions() {
        List<PermissionVO> permissions = permissionService.listAllPermissions();
        return Result.success(permissions);
    }

    /**
     * 获取当前用户菜单树
     */
    @GetMapping("/menu")
    public Result<List<PermissionVO>> getCurrentUserMenus() {
        Long userId = SecurityUtils.getRequiredCurrentUserId();
        List<PermissionVO> menus = permissionService.getMenuTreeByUserId(userId);
        return Result.success(menus);
    }
}

