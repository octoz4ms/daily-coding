package com.example.ssd.service;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

//    private final RedissonClient redissonClient;
//    private final RedisTemplate redisTemplate;
//
//
//    public RateLimitService(RedissonClient redissonClient, RedisTemplate redisTemplate) {
//        this.redisTemplate = redisTemplate;
//        this.redissonClient = redissonClient;
//    }
//
//    /**
//     * Custom 限流
//     *
//     * @param key
//     * @param permitPerSecond 每秒请求数量限制
//     * @return
//     */
//    public boolean isAllowedCustom(String key, int permitPerSecond) {
//        long currentTime = System.currentTimeMillis();
//        redisTemplate.opsForZSet().add(key, currentTime, currentTime);
//        redisTemplate.expire(key, 1, TimeUnit.SECONDS);
//        return redisTemplate.opsForZSet().rangeByScore(key, 0, currentTime).size() <= permitPerSecond;
//    }
//
//    /**
//     * Redisson 限流
//     *
//     * @param key
//     * @param permitPerSecond 每秒请求数量限制
//     * @return
//     */
//    public boolean isAllowed(String key, int permitPerSecond) {
//        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
//        // 初始化限流规则，每秒 permitsPerSecond 个许可，采用平滑限流策略
//        rateLimiter.trySetRate(RateType.OVERALL, permitPerSecond, 1, RateIntervalUnit.SECONDS);
//        return rateLimiter.tryAcquire();
//    }
}
