package com.example.feign.config;

import com.example.feign.fallback.UserFeignClientFallbackFactory;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@ConditionalOnClass(feign.Logger.class)
public class FeignCoreAutoConfiguration {

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
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    long start = System.currentTimeMillis();
                    System.out.println("OkHttp request -> " + request.method() + " " + request.url());
                    Response response = chain.proceed(request);
                    long cost = System.currentTimeMillis() - start;
                    System.out.println("OkHttp response <- " + response.code() + " cost=" + cost + "ms");
                    return response;
                })
                .build();
    }
}
