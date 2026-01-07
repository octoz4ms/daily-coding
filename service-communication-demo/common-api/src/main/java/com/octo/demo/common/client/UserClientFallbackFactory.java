package com.octo.demo.common.client;

import com.octo.demo.common.dto.Result;
import com.octo.demo.common.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务 Feign 降级工厂
 * <p>
 * 【标准规范】使用 FallbackFactory 而非 Fallback
 * <p>
 * 优势：
 * 1. 可以获取到调用失败的异常信息
 * 2. 可以根据不同异常类型返回不同的降级结果
 * 3. 便于日志记录和问题排查
 */
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallbackFactory.class);

    @Override
    public UserClient create(Throwable cause) {
        // 记录异常日志
        log.error("调用用户服务失败，触发降级处理: {}", cause.getMessage());

        return new UserClient() {
            @Override
            public Result<UserDTO> getUserById(Long id) {
                log.warn("getUserById 降级处理，用户ID: {}", id);
                return Result.fail("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<UserDTO>> listUsers() {
                log.warn("listUsers 降级处理");
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<UserDTO>> getUsersByIds(List<Long> ids) {
                log.warn("getUsersByIds 降级处理，用户IDs: {}", ids);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<UserDTO> createUser(UserDTO user) {
                log.warn("createUser 降级处理");
                return Result.fail("用户服务暂时不可用，无法创建用户");
            }

            @Override
            public Result<UserDTO> updateUser(Long id, UserDTO user) {
                log.warn("updateUser 降级处理，用户ID: {}", id);
                return Result.fail("用户服务暂时不可用，无法更新用户");
            }

            @Override
            public Result<Void> deleteUser(Long id) {
                log.warn("deleteUser 降级处理，用户ID: {}", id);
                return Result.fail("用户服务暂时不可用，无法删除用户");
            }
        };
    }
}

