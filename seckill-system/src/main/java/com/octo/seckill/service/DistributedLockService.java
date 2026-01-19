package com.octo.seckill.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务
 * 
 * 基于Redisson实现分布式锁
 * 
 * 面试要点：为什么选择Redisson？
 * 
 * 1. 可重入锁
 *    - 同一线程可以多次获取同一把锁
 * 
 * 2. 锁续期（看门狗机制）
 *    - 默认30秒过期，每10秒自动续期
 *    - 防止业务执行时间过长导致锁提前释放
 * 
 * 3. 红锁（RedLock）
 *    - 多节点加锁，防止Redis主从切换导致锁失效
 * 
 * 4. 公平锁
 *    - 按请求顺序获取锁
 * 
 * 对比其他方案：
 * - SETNX + EXPIRE：非原子操作，不可重入
 * - SET NX PX：原子操作，但无续期，不可重入
 * - RedLock：复杂，需要多个Redis实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    @Value("${seckill.stock.lock-prefix}")
    private String lockPrefix;

    @Value("${seckill.stock.lock-wait-time}")
    private long lockWaitTime;

    @Value("${seckill.stock.lock-lease-time}")
    private long lockLeaseTime;

    /**
     * 尝试获取锁并执行业务
     * 
     * @param lockKey 锁的key
     * @param supplier 业务逻辑
     * @return 业务执行结果，获取锁失败返回null
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        String fullKey = lockPrefix + lockKey;
        RLock lock = redissonClient.getLock(fullKey);
        
        try {
            // 尝试获取锁
            boolean acquired = lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("获取分布式锁失败: key={}", fullKey);
                return null;
            }
            
            log.debug("获取分布式锁成功: key={}", fullKey);
            return supplier.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: key={}", fullKey);
            return null;
        } finally {
            // 只有当前线程持有锁才释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁: key={}", fullKey);
            }
        }
    }

    /**
     * 获取用户+活动维度的锁
     * 防止同一用户重复秒杀
     */
    public <T> T executeWithUserActivityLock(Long userId, Long activityId, Supplier<T> supplier) {
        String lockKey = "user:" + userId + ":activity:" + activityId;
        return executeWithLock(lockKey, supplier);
    }

    /**
     * 获取活动库存锁
     * 用于库存操作的串行化（可选方案）
     */
    public <T> T executeWithStockLock(Long activityId, Supplier<T> supplier) {
        String lockKey = "stock:" + activityId;
        return executeWithLock(lockKey, supplier);
    }
}

