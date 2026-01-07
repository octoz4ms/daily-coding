package com.octo.demo.common.client;

import com.octo.demo.common.dto.Result;
import com.octo.demo.common.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端接口
 * <p>
 * 【标准规范】OpenFeign 声明式服务调用
 * <p>
 * 使用方式一：指定 url（不依赖注册中心，适合开发测试或简单部署）
 * 使用方式二：使用 name + 注册中心（生产推荐，支持负载均衡和服务发现）
 * <p>
 * 属性说明：
 * - name: 服务名称（必须），用于服务发现或日志
 * - url: 服务地址（可选），直接指定服务URL，适合无注册中心场景
 * - path: 统一路径前缀（可选）
 * - fallback: 降级处理类（可选）
 * - fallbackFactory: 降级工厂类（可选，可获取异常信息）
 */
@FeignClient(
        name = "provider-service",
        url = "${provider.service.url:http://localhost:8081}",
        path = "/api/users",
        fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    /**
     * 根据ID获取用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    @GetMapping
    Result<List<UserDTO>> listUsers();

    /**
     * 根据ID列表批量获取用户
     *
     * @param ids 用户ID列表
     * @return 用户列表
     */
    @GetMapping("/batch")
    Result<List<UserDTO>> getUsersByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping
    Result<UserDTO> createUser(@RequestBody UserDTO user);

    /**
     * 更新用户
     *
     * @param id   用户ID
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    Result<UserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UserDTO user);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    Result<Void> deleteUser(@PathVariable("id") Long id);
}

