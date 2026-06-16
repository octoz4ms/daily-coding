package com.example.feign.fallback;

import com.example.common.result.Result;
import com.example.feign.client.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * UserFeignClient 降级工厂
 *
 * <p>配合 Sentinel 在 Feign 侧的熔断限流（{@code spring.cloud.openfeign.sentinel.enabled=true}），
 * 当被调用方触发限流/降级时，会执行 {@code create(Throwable cause)} 创建降级实例。
 *
 * <p>异常分类通过类名识别，避免对 Sentinel 的强依赖（feign-core 模块不直接依赖 sentinel-core）。
 * <ul>
 *   <li>类名含 "FlowException" / "ParamFlowException" - 流控</li>
 *   <li>类名含 "DegradeException" - 熔断降级</li>
 *   <li>类名含 "SystemBlockException" - 系统保护</li>
 *   <li>类名含 "AuthorityException" - 授权规则</li>
 *   <li>其他 - 普通业务异常（用户服务宕机/超时/5xx 等）</li>
 * </ul>
 */
@Slf4j
@Component
public class UserFeignClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return id -> {
            String exClass = cause == null ? "null" : cause.getClass().getName();

            if (exClass.contains("ParamFlowException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 热点参数限流, id={}", id);
                return Result.fail(429, "[热点限流] 当前用户访问过于频繁，请稍后重试");
            }
            if (exClass.contains("FlowException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 流控(Flow), id={}", id);
                return Result.fail(429, "[限流] 用户服务 QPS 超限，请稍后重试");
            }
            if (exClass.contains("DegradeException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 熔断降级, id={}", id);
                return Result.fail(503, "[熔断] 用户服务暂时不可用，已降级");
            }
            if (exClass.contains("SystemBlockException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 系统保护, id={}", id);
                return Result.fail(503, "[系统保护] 用户服务系统负载过高，已降级");
            }
            if (exClass.contains("AuthorityException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 授权规则, id={}", id);
                return Result.fail(403, "[授权] 没有访问用户服务的权限");
            }
            if (exClass.contains("BlockException")) {
                log.warn("[UserFeignClient] 触发 Sentinel 其他限流, id={}, type={}", id, exClass);
                return Result.fail(429, "[限流] 用户服务访问受限");
            }
            // 非 Sentinel 异常：网络中断/超时/被调用方 5xx 等
            log.error("[UserFeignClient] 调用异常降级, id={}, reason={}", id,
                    cause == null ? "null" : cause.getMessage());
            return Result.fail(503, "用户服务暂时不可用，请稍后重试");
        };
    }
}
