package com.example.order.service;

import com.example.common.entity.User;
import com.example.common.result.Result;
import com.example.feign.client.UserFeignClient;
import com.example.order.exception.RemoteServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserRemoteService {

    private static final String DEFAULT_USER_PREFIX = "default-user-";

    @Resource
    private UserFeignClient userFeignClient;

    public User getUserStrict(Long userId) {
        Result<User> userResult = userFeignClient.getUserById(userId);
        if (!isSuccess(userResult) || userResult.getData() == null) {
            log.warn("user-service degraded, api=getUserById, userId={}, code={}, message={}",
                    userId,
                    userResult == null ? null : userResult.getCode(),
                    userResult == null ? null : userResult.getMessage());
            throw new RemoteServiceException(503, "用户服务异常，暂时无法处理当前请求");
        }
        return userResult.getData();
    }

    public User getUserWithDefault(Long userId) {
        Result<User> userResult = userFeignClient.getUserById(userId);
        if (isSuccess(userResult) && userResult.getData() != null) {
            return userResult.getData();
        }

        log.warn("user-service degraded, fallback to default user, userId={}, code={}, message={}",
                userId,
                userResult == null ? null : userResult.getCode(),
                userResult == null ? null : userResult.getMessage());
        return buildDefaultUser(userId);
    }

    public boolean isDefaultUser(User user) {
        return user != null && user.getUsername() != null && user.getUsername().startsWith(DEFAULT_USER_PREFIX);
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && Integer.valueOf(200).equals(result.getCode());
    }

    private User buildDefaultUser(Long userId) {
        return new User(userId, DEFAULT_USER_PREFIX + userId, "unknown@example.com", "N/A");
    }
}
