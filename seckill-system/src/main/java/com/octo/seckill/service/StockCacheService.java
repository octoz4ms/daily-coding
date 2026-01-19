package com.octo.seckill.service;

import com.octo.seckill.entity.SeckillActivity;
import com.octo.seckill.mapper.SeckillActivityMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 库存缓存服务
 * 
 * 核心功能：Redis缓存预热 + 库存预扣减
 * 
 * 面试要点 - 如何保证库存不超卖？
 * 
 * 方案一：Redis预扣减 + 数据库最终扣减（本项目采用）
 *   1. 秒杀前将库存预热到Redis
 *   2. 使用Redis DECR原子操作预扣减库存
 *   3. 如果Redis库存>=0，发送MQ消息
 *   4. 消费者使用数据库乐观锁最终扣减
 *   5. 如果数据库扣减失败，Redis库存回滚
 * 
 * 方案二：分布式锁
 *   - 使用Redisson分布式锁串行化扣减操作
 *   - 缺点：并发性能受限
 * 
 * 方案三：数据库乐观锁
 *   - UPDATE ... WHERE stock > 0 AND version = ?
 *   - 缺点：高并发下数据库压力大
 * 
 * 方案四：Redis Lua脚本
 *   - 将判断和扣减合并为原子操作
 *   - 本项目在deductStock方法中使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheService {

    private final StringRedisTemplate redisTemplate;
    private final SeckillActivityMapper activityMapper;

    @Value("${seckill.stock.cache-prefix}")
    private String stockCachePrefix;

    /**
     * 库存预扣减Lua脚本
     * 
     * 面试重点：为什么用Lua脚本？
     * - 保证判断库存和扣减库存的原子性
     * - 避免先GET再DECR导致的并发问题
     */
    private static final String DEDUCT_STOCK_SCRIPT = 
            "local stock = redis.call('GET', KEYS[1]) " +
            "if stock and tonumber(stock) > 0 then " +
            "    redis.call('DECR', KEYS[1]) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";

    /**
     * 应用启动时预热库存到Redis
     */
    @PostConstruct
    public void warmUpStock() {
        log.info("========== 开始预热秒杀库存到Redis ==========");
        
        // 查询所有进行中的秒杀活动
        List<SeckillActivity> activities = activityMapper.selectList(null);
        
        for (SeckillActivity activity : activities) {
            String key = getStockKey(activity.getId());
            redisTemplate.opsForValue().set(
                    key, 
                    String.valueOf(activity.getAvailableStock()),
                    24, 
                    TimeUnit.HOURS
            );
            log.info("预热库存: activityId={}, stock={}", activity.getId(), activity.getAvailableStock());
        }
        
        log.info("========== 秒杀库存预热完成 ==========");
    }

    /**
     * 定时同步库存（每5分钟）
     * 保证Redis与数据库库存的最终一致性
     */
    @Scheduled(fixedRate = 300000)
    public void syncStock() {
        log.debug("开始同步库存...");
        List<SeckillActivity> activities = activityMapper.selectList(null);
        for (SeckillActivity activity : activities) {
            String key = getStockKey(activity.getId());
            String cachedStock = redisTemplate.opsForValue().get(key);
            if (cachedStock != null) {
                int redisStock = Integer.parseInt(cachedStock);
                int dbStock = activity.getAvailableStock();
                if (redisStock != dbStock) {
                    log.warn("库存不一致! activityId={}, redisStock={}, dbStock={}", 
                            activity.getId(), redisStock, dbStock);
                    // 以数据库为准
                    redisTemplate.opsForValue().set(key, String.valueOf(dbStock));
                }
            }
        }
    }

    /**
     * Redis预扣减库存
     * 
     * @param activityId 活动ID
     * @return true-扣减成功 false-库存不足
     */
    public boolean deductStock(Long activityId) {
        String key = getStockKey(activityId);
        
        // 使用Lua脚本原子操作
        Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(DEDUCT_STOCK_SCRIPT, Long.class),
                List.of(key)
        );
        
        boolean success = result != null && result == 1;
        
        if (success) {
            log.debug("Redis库存预扣减成功: activityId={}", activityId);
        } else {
            log.debug("Redis库存不足: activityId={}", activityId);
        }
        
        return success;
    }

    /**
     * 恢复Redis库存
     * 当订单创建失败或取消时调用
     */
    public void restoreStock(Long activityId) {
        String key = getStockKey(activityId);
        redisTemplate.opsForValue().increment(key);
        log.debug("Redis库存已恢复: activityId={}", activityId);
    }

    /**
     * 获取当前库存
     */
    public int getStock(Long activityId) {
        String key = getStockKey(activityId);
        String stock = redisTemplate.opsForValue().get(key);
        return stock != null ? Integer.parseInt(stock) : 0;
    }

    /**
     * 检查库存是否充足
     */
    public boolean hasStock(Long activityId) {
        return getStock(activityId) > 0;
    }

    private String getStockKey(Long activityId) {
        return stockCachePrefix + activityId;
    }
}

