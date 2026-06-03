package com.example.feign.fallback;

import com.example.common.entity.User;
import com.example.common.result.Result;
import com.example.feign.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public Result<User> getUserById(Long id) {
                log.error("获取用户失败, id={}, 原因: {}", id, cause.getMessage());
                return Result.success(new User(1L, "默认", null, null));
            }
        };
    }
}