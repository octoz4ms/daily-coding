package com.octo.demo.provider.controller;

import com.octo.demo.common.dto.Result;
import com.octo.demo.common.dto.UserDTO;
import com.octo.demo.provider.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * <p>
 * 【标准规范】RESTful API 设计
 * <p>
 * 路径命名：使用复数名词（/users）
 * HTTP方法：GET查询、POST创建、PUT更新、DELETE删除
 * 响应格式：统一使用Result包装
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        log.info("获取用户，ID: {}", id);
        UserDTO user = userService.getUserById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<List<UserDTO>> listUsers() {
        log.info("获取用户列表");
        List<UserDTO> users = userService.listUsers();
        return Result.success(users);
    }

    /**
     * 批量获取用户
     */
    @GetMapping("/batch")
    public Result<List<UserDTO>> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        log.info("批量获取用户，IDs: {}", ids);
        List<UserDTO> users = userService.getUsersByIds(ids);
        return Result.success(users);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> createUser(@RequestBody UserDTO user) {
        log.info("创建用户: {}", user);
        UserDTO created = userService.createUser(user);
        return Result.success("创建成功", created);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO user) {
        log.info("更新用户，ID: {}, 数据: {}", id, user);
        user.setId(id);
        UserDTO updated = userService.updateUser(user);
        return Result.success("更新成功", updated);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("删除用户，ID: {}", id);
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }
}

