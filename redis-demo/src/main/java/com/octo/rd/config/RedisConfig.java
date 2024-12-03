package com.octo.rd.config;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://111.229.106.212:6379").setPassword("Shuai3198@").setDatabase(1);
        return Redisson.create(config);
    }

    @Bean
    public RBloomFilter<String> usernameBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("usernameBloomFilter");
        bloomFilter.tryInit(1000, 0.1);
        return bloomFilter;
    }

    @Bean
    public RBloomFilter<String> productBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("productBloomFilter");
        bloomFilter.tryInit(1000, 0.1);
        return bloomFilter;
    }
}
