package com.octo.ssd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.octo.ssd.common.Result;
import com.octo.ssd.dto.UserDTO;
import com.octo.ssd.entity.User;
import com.octo.ssd.service.IUserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 *
 * @author octo
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 分页查询用户列表
     *
     * @param page     当前页（默认1）
     * @param size     每页大小（默认10）
     * @param username 用户名（模糊查询）
     * @return 用户分页列表
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:list')")
    public Result<IPage<User>> getUserPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username) {
        IPage<User> userPage = userService.getUserPage(page, size, username);
        return Result.success(userPage);
    }

    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:query')")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserDetail(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 创建的用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:add')")
    public Result<User> createUser(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.createUser(userDTO);
        return Result.success("用户创建成功", user);
    }

    /**
     * 更新用户
     *
     * @param id      用户ID
     * @param userDTO 用户信息
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:edit')")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        User user = userService.updateUser(userDTO);
        return Result.success("用户更新成功", user);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:delete')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }

    /**
     * 为用户分配角色
     *
     * @param id      用户ID
     * @param roleIds 角色ID列表
     * @return 分配结果
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:assignRole')")
    public Result<User> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        // 先删除原有角色再分配新角色
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setRoleIds(roleIds);
        User user = userService.updateUser(userDTO);
        return Result.success("角色分配成功", user);
    }
}
