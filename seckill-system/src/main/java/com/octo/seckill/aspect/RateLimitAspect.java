package com.octo.seckill.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.octo.seckill.annotation.RateLimit;
import com.octo.seckill.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 
 * 基于Guava RateLimiter实现令牌桶限流
 * 
 * 面试要点：
 * 1. 为什么用ConcurrentHashMap缓存RateLimiter？
 *    - 每个接口有独立的限流器，避免相互影响
 *    - 线程安全，支持高并发访问
 * 2. 为什么用tryAcquire而不是acquire？
 *    - acquire会阻塞等待，影响响应时间
 *    - tryAcquire立即返回，超时则快速失败
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * 限流器缓存 - 每个接口一个限流器
     */
    private final Map<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    @Around("@annotation(com.octo.seckill.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 获取限流key
        String key = rateLimit.key().isEmpty() ? 
                method.getDeclaringClass().getName() + "." + method.getName() : 
                rateLimit.key();

        // 获取或创建限流器
        RateLimiter rateLimiter = rateLimiterCache.computeIfAbsent(key, 
                k -> RateLimiter.create(rateLimit.permitsPerSecond()));

        // 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire(rateLimit.timeout(), TimeUnit.MILLISECONDS);

        if (!acquired) {
            log.warn("接口限流触发: key={}, permitsPerSecond={}", key, rateLimit.permitsPerSecond());
            return Result.rateLimited();
        }

        return point.proceed();
    }
}

