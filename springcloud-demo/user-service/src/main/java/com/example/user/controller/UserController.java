package com.example.user.controller;

import com.example.common.entity.User;
import com.example.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    // 模拟数据库
    private static final Map<Long, User> USER_MAP = new HashMap<>();

    static {
        USER_MAP.put(1L, new User(1L, "张三", "zhangsan@example.com", "13800138001"));
        USER_MAP.put(2L, new User(2L, "李四", "lisi@example.com", "13800138002"));
        USER_MAP.put(3L, new User(3L, "王五", "wangwu@example.com", "13800138003"));
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable(name = "id") Long id) {
        User user = USER_MAP.get(id);
        if (user != null) {
            return Result.success(user);
        }
        return Result.fail("用户不存在");
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/list")
    public Result<Map<Long, User>> listUsers() {
        return Result.success(USER_MAP);
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<String> createUser(@RequestBody User user) {
        USER_MAP.put(user.getId(), user);
        return Result.success("用户创建成功");
    }
}
