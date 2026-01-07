package com.octo.demo.consumer.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 全局配置类
 * <p>
 * 【标准规范】Feign 配置最佳实践
 * <p>
 * 配置项说明：
 * 1. Logger.Level - 日志级别
 * 2. Retryer - 重试策略
 * 3. Request.Options - 超时配置
 * 4. ErrorDecoder - 错误解码器（可自定义）
 * 5. RequestInterceptor - 请求拦截器（可添加认证头等）
 */
@Configuration
public class FeignConfig {

    /**
     * Feign 日志级别配置
     * <p>
     * NONE: 不记录日志（默认）
     * BASIC: 仅记录请求方法、URL、响应状态码和执行时间
     * HEADERS: 记录 BASIC + 请求和响应头
     * FULL: 记录完整的请求和响应（包含body）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * 重试策略配置
     * <p>
     * 默认不重试，可根据需要配置
     * Retryer.Default: 重试5次，初始间隔100ms，最大间隔1s
     */
    @Bean
    public Retryer feignRetryer() {
        // 不重试
        return Retryer.NEVER_RETRY;
        
        // 自定义重试策略
        // return new Retryer.Default(100, 1000, 3);
    }

    /**
     * 超时配置
     * <p>
     * 优先级: 接口级别 > 客户端级别 > 默认级别
     */
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,  // 连接超时
                5, TimeUnit.SECONDS,  // 读取超时
                true                   // followRedirects
        );
    }

    /**
     * 请求拦截器示例（可用于添加认证头、链路追踪等）
     */
    // @Bean
    // public RequestInterceptor requestInterceptor() {
    //     return template -> {
    //         // 添加认证头
    //         template.header("Authorization", "Bearer xxx");
    //         // 添加链路追踪ID
    //         template.header("X-Trace-Id", UUID.randomUUID().toString());
    //     };
    // }
}

