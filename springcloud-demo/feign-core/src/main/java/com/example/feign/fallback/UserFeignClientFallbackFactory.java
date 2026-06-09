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
            log.warn("[UserFeignClient] 服务降级触发, method=getUserById, id={}, reason={}",
                    id, cause.getMessage());
            return Result.fail(503, "用户服务暂时不可用，请稍后重试");
        };
    }
}
