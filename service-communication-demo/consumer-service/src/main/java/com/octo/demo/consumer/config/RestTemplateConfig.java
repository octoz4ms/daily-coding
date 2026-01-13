package com.octo.demo.consumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
     * 使用 SimpleClientHttpRequestFactory 配置超时
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：5秒
        factory.setConnectTimeout(5000);
        // 读取超时：5秒
        factory.setReadTimeout(5000);
        
        return new RestTemplate(factory);
    }
}

