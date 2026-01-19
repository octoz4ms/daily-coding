package com.octo.seckill.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 
 * 使用令牌桶算法实现接口限流
 * 
 * 面试要点：
 * 1. 令牌桶 vs 漏桶算法的区别
 *    - 令牌桶允许突发流量，漏桶平滑输出
 * 2. 为什么选择令牌桶？
 *    - 秒杀场景需要允许一定的突发流量
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每秒允许的请求数
     */
    double permitsPerSecond() default 100;

    /**
     * 获取令牌的超时时间（毫秒）
     */
    long timeout() default 500;

    /**
     * 限流key，支持SpEL表达式
     * 默认以方法名作为key
     */
    String key() default "";
}

