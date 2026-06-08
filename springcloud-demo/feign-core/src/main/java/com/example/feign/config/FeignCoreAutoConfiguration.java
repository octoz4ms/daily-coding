package com.example.feign.config;

import com.example.feign.fallback.UserFeignClientFallbackFactory;
import feign.Logger;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@ConditionalOnClass(Logger.class)
public class FeignCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    @ConditionalOnMissingBean(UserFeignClientFallbackFactory.class)
    public UserFeignClientFallbackFactory userFeignClientFallbackFactory() {
        return new UserFeignClientFallbackFactory();
    }

    @Bean
    @ConditionalOnClass(OkHttpClient.class)
    @ConditionalOnMissingBean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }
}
