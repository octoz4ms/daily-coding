package com.octo.ssd.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 *
 * <p>企业通用封装，支持字符串、对象、Hash、List、Set 等常用缓存操作
 * 统一使用 JSON 序列化，避免类型转换问题</p>
 * <p>
 * 使用示例：
 * <pre>
 *     redisUtil.set("user:1", user, 10, TimeUnit.MINUTES);
 *     User user = redisUtil.get("user:1", User.class);
 * </pre>
 */
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /* ============================= 通用操作 ============================= */

    /**
     * 设置缓存（带过期时间）
     */
    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, toJson(value), timeout, unit);
    }

    /**
     * 设置缓存（无过期时间）
     */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, toJson(value));
    }

    /**
     * 获取缓存（对象）
     */
    public <T> T get(String key, Class<T> clazz) {
        Object json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        return fromJson(json.toString(), clazz);
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    /* ============================= Hash ============================= */

    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, toJson(value));
    }

    public <T> T hGet(String key, String field, Class<T> clazz) {
        Object json = redisTemplate.opsForHash().get(key, field);
        return json == null ? null : fromJson(json.toString(), clazz);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void hDel(String key, Object... fields) {
        redisTemplate.opsForHash().delete(key, fields);
    }

    /* ============================= List ============================= */

    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, toJson(value));
    }

    public <T> T rPop(String key, Class<T> clazz) {
        Object json = redisTemplate.opsForList().rightPop(key);
        return json == null ? null : fromJson(json.toString(), clazz);
    }

    public <T> T lIndex(String key, long index, Class<T> clazz) {
        Object json = redisTemplate.opsForList().index(key, index);
        return json == null ? null : fromJson(json.toString(), clazz);
    }

    /* ============================= Set ============================= */

    public void sAdd(String key, Object... values) {
        for (Object value : values) {
            redisTemplate.opsForSet().add(key, toJson(value));
        }
    }

    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, toJson(value));
    }

    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /* ============================= 分布式锁 ============================= */

    /**
     * 尝试获取分布式锁
     */
    public Boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 释放分布式锁（保证 value 匹配，避免误删）
     */
    public Boolean unlock(String key, String value) {
        Object currentValue = redisTemplate.opsForValue().get(key);
        if (value.equals(currentValue)) {
            return redisTemplate.delete(key);
        }
        return false;
    }

    /* ============================= JSON 封装 ============================= */

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("RedisUtil 序列化失败", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("RedisUtil 反序列化失败", e);
        }
    }
}
