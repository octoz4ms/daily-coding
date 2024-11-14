package com.example.ssd.config;


import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RBloomFilterConfiguration {

    @Bean
    public RBloomFilter<String> userRegisterBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("userRegisterBloomFilter");
        bloomFilter.tryInit(1000, 0.01);
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> ageRegisterBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("ageRegisterBloomFilter");
        bloomFilter.tryInit(10000, 0.01);
        return bloomFilter;
    }
}
