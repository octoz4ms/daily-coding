package com.example.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.entity.Stock;
import com.example.payment.mapper.StockMapper;
import com.example.payment.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 库存服务实现类
 */
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock:available:";

    @Override
    public boolean checkStock(Long productId, Integer quantity) {
        // 先查Redis缓存
        String key = STOCK_KEY_PREFIX + productId;
        String stockStr = redisTemplate.opsForValue().get(key);
        
        if (stockStr != null) {
            int availableStock = Integer.parseInt(stockStr);
            return availableStock >= quantity;
        }

        // 缓存不存在，查数据库
        Integer availableStock = getAvailableStock(productId);
        return availableStock != null && availableStock >= quantity;
    }

    @Override
    public boolean lockStock(Long productId, Integer quantity) {
        log.info("锁定库存，productId: {}, quantity: {}", productId, quantity);

        // 先尝试扣减Redis库存（快速失败）
        String key = STOCK_KEY_PREFIX + productId;
        Long result = redisTemplate.opsForValue().decrement(key, quantity);
        
        if (result != null && result < 0) {
            // Redis库存不足，恢复
            redisTemplate.opsForValue().increment(key, quantity);
            log.warn("Redis库存不足，productId: {}", productId);
            return false;
        }

        // 数据库扣减（乐观锁）
        int updated = stockMapper.lockStock(productId, quantity);
        
        if (updated == 0) {
            // 数据库扣减失败，恢复Redis库存
            if (result != null) {
                redisTemplate.opsForValue().increment(key, quantity);
            }
            log.warn("数据库库存扣减失败，productId: {}", productId);
            return false;
        }

        log.info("库存锁定成功，productId: {}, quantity: {}", productId, quantity);
        return true;
    }

    @Override
    public boolean releaseStock(Long productId, Integer quantity) {
        log.info("释放库存，productId: {}, quantity: {}", productId, quantity);

        // 数据库释放库存
        int updated = stockMapper.releaseStock(productId, quantity);
        
        if (updated > 0) {
            // 恢复Redis库存
            String key = STOCK_KEY_PREFIX + productId;
            redisTemplate.opsForValue().increment(key, quantity);
            
            log.info("库存释放成功，productId: {}, quantity: {}", productId, quantity);
            return true;
        }

        log.warn("库存释放失败，productId: {}", productId);
        return false;
    }

    @Override
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("扣减库存，productId: {}, quantity: {}", productId, quantity);

        // 将锁定库存转为已售库存
        int updated = stockMapper.deductStock(productId, quantity);
        
        if (updated > 0) {
            log.info("库存扣减成功，productId: {}, quantity: {}", productId, quantity);
            return true;
        }

        log.warn("库存扣减失败，productId: {}", productId);
        return false;
    }

    @Override
    public Integer getAvailableStock(Long productId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProductId, productId);
        Stock stock = stockMapper.selectOne(wrapper);
        
        if (stock != null) {
            // 同步到Redis
            String key = STOCK_KEY_PREFIX + productId;
            redisTemplate.opsForValue().set(key, String.valueOf(stock.getAvailableStock()));
            return stock.getAvailableStock();
        }
        
        return 0;
    }
}

