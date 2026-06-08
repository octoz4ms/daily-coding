package com.example.feign.client;

import com.example.common.entity.User;
import com.example.common.result.Result;
import com.example.feign.constant.FeignClientConstants;
import com.example.feign.fallback.UserFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = FeignClientConstants.USER_SERVICE,
        path = FeignClientConstants.USER_SERVICE_PATH,
        fallbackFactory = UserFeignClientFallbackFactory.class
)
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
