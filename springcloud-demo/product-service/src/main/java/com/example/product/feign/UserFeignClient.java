package com.example.product.feign;

import com.example.common.entity.User;
import com.example.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient( name = "user-service", path = "/user")
public interface UserFeignClient {

    @GetMapping("/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
