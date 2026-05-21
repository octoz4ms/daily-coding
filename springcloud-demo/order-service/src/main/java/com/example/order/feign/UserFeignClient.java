package com.example.order.feign;

import com.example.common.entity.User;
import com.example.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 * 用于远程调用 user-service 服务
 */
@FeignClient(value = "user-service", path = "/user")
public interface UserFeignClient {

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
