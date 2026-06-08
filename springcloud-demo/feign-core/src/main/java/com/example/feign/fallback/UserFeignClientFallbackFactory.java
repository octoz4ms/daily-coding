package com.example.feign.fallback;

import com.example.common.result.Result;
import com.example.feign.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return id -> {
            log.error("[UserFeignClient] getUserById fallback, id={}", id, cause);
            return Result.fail(503, "用户服务暂时不可用，请稍后重试");
        };
    }
}
