package com.octo.seckill.service;

import com.octo.seckill.common.Result;
import com.octo.seckill.dto.SeckillMessage;
import com.octo.seckill.dto.SeckillRequest;
import com.octo.seckill.entity.SeckillActivity;
import com.octo.seckill.mapper.SeckillActivityMapper;
import com.octo.seckill.mapper.SeckillOrderMapper;
import com.octo.seckill.mq.SeckillMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀核心服务
 * 
 * 秒杀流程设计（面试重点）：
 * 
 * 1. 请求进入 -> 接口限流（令牌桶）
 * 2. 校验活动状态
 * 3. 校验用户是否已秒杀（Redis + 数据库双重校验）
 * 4. 预扣减Redis库存（Lua脚本保证原子性）
 * 5. 发送MQ消息（异步削峰）
 * 6. 立即返回"排队中"
 * 7. 消费者异步创建订单（数据库乐观锁扣减库存）
 * 
 * 库存超卖解决方案总结：
 * 
 * 第一层防护：Redis Lua脚本原子预扣减
 *   - 先判断库存 > 0，再执行DECR
 *   - 保证不会出现负库存
 * 
 * 第二层防护：数据库乐观锁
 *   - UPDATE ... WHERE available_stock > 0
 *   - 即使Redis和数据库数据不一致，数据库也能兜底
 * 
 * 第三层防护：唯一索引
 *   - (user_id, activity_id) 唯一索引
 *   - 防止同一用户重复下单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillOrderMapper orderMapper;
    private final StockCacheService stockCacheService;
    private final DistributedLockService lockService;
    private final SeckillMessageProducer messageProducer;
    private final StringRedisTemplate redisTemplate;

    /**
     * 已秒杀用户缓存前缀
     */
    private static final String SECKILL_USER_PREFIX = "seckill:user:";

    /**
     * 执行秒杀
     * 
     * @param request 秒杀请求
     * @return 秒杀结果
     */
    public Result<String> doSeckill(SeckillRequest request) {
        Long userId = request.getUserId();
        Long activityId = request.getActivityId();

        log.info("秒杀请求: userId={}, activityId={}", userId, activityId);

        // 1. 校验活动状态
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return Result.fail("活动不存在");
        }
        if (!activity.isActive()) {
            return activity.getStatus() == 0 ? Result.activityNotStart() : Result.activityEnded();
        }

        // 2. 快速判断库存（内存标记，可选优化）
        if (!stockCacheService.hasStock(activityId)) {
            return Result.soldOut();
        }

        // 3. 使用分布式锁，防止同一用户重复秒杀
        return lockService.executeWithUserActivityLock(userId, activityId, () -> {
            
            // 4. 校验用户是否已秒杀（Redis缓存 + 数据库双重检查）
            String userSeckillKey = SECKILL_USER_PREFIX + activityId + ":" + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userSeckillKey))) {
                return Result.repeatSeckill();
            }
            
            // 数据库二次校验
            int orderCount = orderMapper.countByUserAndActivity(userId, activityId);
            if (orderCount > 0) {
                // 缓存用户已秒杀状态
                redisTemplate.opsForValue().set(userSeckillKey, "1", 24, TimeUnit.HOURS);
                return Result.repeatSeckill();
            }

            // 5. Redis预扣减库存
            boolean deducted = stockCacheService.deductStock(activityId);
            if (!deducted) {
                return Result.soldOut();
            }

            // 6. 生成订单号
            String orderNo = generateOrderNo(userId, activityId);

            // 7. 发送MQ消息，异步创建订单
            SeckillMessage message = SeckillMessage.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .productId(activity.getProductId())
                    .seckillPrice(activity.getSeckillPrice())
                    .orderNo(orderNo)
                    .build();
            
            messageProducer.sendSeckillMessage(message);

            // 8. 标记用户已秒杀
            redisTemplate.opsForValue().set(userSeckillKey, "1", 24, TimeUnit.HOURS);

            log.info("秒杀成功，订单排队中: userId={}, orderNo={}", userId, orderNo);
            
            return Result.success("秒杀成功，订单排队中", orderNo);
        });
    }

    /**
     * 查询秒杀结果
     * 
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 订单号或排队状态
     */
    public Result<String> getSeckillResult(Long userId, Long activityId) {
        // 查询订单
        int count = orderMapper.countByUserAndActivity(userId, activityId);
        
        if (count > 0) {
            return Result.success("秒杀成功");
        }
        
        // 检查是否在排队
        String userSeckillKey = SECKILL_USER_PREFIX + activityId + ":" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userSeckillKey))) {
            return Result.success("排队中，请稍后查询");
        }
        
        return Result.fail("未参与秒杀");
    }

    /**
     * 生成订单号
     * 格式：年月日时分秒 + 活动ID(4位) + 用户ID后4位 + 随机数(4位)
     */
    private String generateOrderNo(Long userId, Long activityId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String activityPart = String.format("%04d", activityId % 10000);
        String userPart = String.format("%04d", userId % 10000);
        String randomPart = UUID.randomUUID().toString().substring(0, 4);
        return timestamp + activityPart + userPart + randomPart;
    }
}

