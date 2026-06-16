package com.example.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.common.entity.User;
import com.example.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 *
 * <p>所有接口均挂载 {@link SentinelResource}，在 {@code user-service} 侧声明流控资源，
 * 配合 order-service 侧的 Sentinel 资源，可演示：
 * <ul>
 *   <li>order-service -> user-service 调用链路的客户端限流</li>
 *   <li>user-service 服务端入口的限流（被调用方）</li>
 *   <li>Feign + Sentinel Fallback 链路（order-service 触发限流时返回降级结果）</li>
 * </ul>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Map<Long, User> USER_MAP = new HashMap<>();

    static {
        USER_MAP.put(1L, new User(1L, "张三", "zhangsan@example.com", "13800138001"));
        USER_MAP.put(2L, new User(2L, "李四", "lisi@example.com", "13800138002"));
        USER_MAP.put(3L, new User(3L, "王五", "wangwu@example.com", "13800138003"));
    }

    /**
     * 根据ID获取用户
     * <p>资源名 {@code user:getById}，流控规则见 {@code SentinelUserRuleInitializer}。
     */
    @GetMapping("/{id}")
    @SentinelResource(value = "user:getById", blockHandler = "getByIdBlockHandler", fallback = "getByIdFallback")
    public Result<User> getUserById(@PathVariable(name = "id") Long id) {
        User user = USER_MAP.get(id);
        if (user != null) {
            return Result.success(user);
        }
        return Result.fail("用户不存在");
    }

    public Result<User> getByIdBlockHandler(Long id, BlockException ex) {
        return Result.fail(429, "user-service [getById] 触发限流，id=" + id);
    }

    public Result<User> getByIdFallback(Long id, Throwable ex) {
        return Result.fail(500, "user-service [getById] 业务异常降级，id=" + id);
    }

    @GetMapping("/list")
    @SentinelResource(value = "user:list", blockHandler = "listBlockHandler")
    public Result<Map<Long, User>> listUsers() {
        return Result.success(USER_MAP);
    }

    public Result<Map<Long, User>> listBlockHandler(BlockException ex) {
        return Result.fail(429, "user-service [list] 触发限流");
    }

    @PostMapping
    public Result<String> createUser(@RequestBody User user) {
        USER_MAP.put(user.getId(), user);
        return Result.success("用户创建成功");
    }
}
