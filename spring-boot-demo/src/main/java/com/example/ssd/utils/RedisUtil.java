package com.example.ssd.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private ThreadLocal<String> threadLocal = new ThreadLocal<>();

    // 设置值
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 设置值并指定过期时间
    public void set(String key, Object value, long timout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timout, unit);
    }

    // 获取值
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 检查键是否存在
    public boolean hashKey(String key) {
        return redisTemplate.hasKey(key);
    }

    // 删除键
    public boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // 设置键的过期时间
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }


    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        String uuid = UUID.randomUUID().toString();
        threadLocal.set(uuid);
        return redisTemplate.opsForValue().setIfAbsent(key, uuid, timeout, unit);
    }

    public void releaseLock(String key) {
        if (threadLocal.get().equals(redisTemplate.opsForValue().get(key))) redisTemplate.delete(key);
    }
}
