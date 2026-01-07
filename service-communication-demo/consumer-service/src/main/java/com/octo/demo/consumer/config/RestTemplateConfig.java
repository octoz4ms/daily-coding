package com.octo.demo.consumer.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置类
 * <p>
 * 【标准规范】RestTemplate 服务调用配置
 * <p>
 * 1. 配置连接超时和读取超时
 * 2. 可添加拦截器进行日志、认证等处理
 * 3. 生产环境建议使用连接池（如 HttpClient、OkHttp）
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean
     * <p>
     * 使用 RestTemplateBuilder 构建，支持自定义配置
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 连接超时
                .connectTimeout(Duration.ofSeconds(5))
                // 读取超时
                .readTimeout(Duration.ofSeconds(5))
                // 可添加拦截器
                // .interceptors(new LoggingInterceptor())
                .build();
    }

    /**
     * 如果需要更细粒度的控制，可以手动创建 RequestFactory
     */
    // @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return factory;
    }
}

