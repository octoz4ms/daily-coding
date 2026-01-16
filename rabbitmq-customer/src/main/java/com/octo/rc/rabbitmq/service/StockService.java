package com.octo.rc.rabbitmq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库存服务
 * 负责商品库存的锁定和释放
 */
@Service
@Slf4j
public class StockService {

    /**
     * 模拟商品库存（实际项目中应该使用数据库 + Redis）
     * key: 商品ID, value: 库存数量
     */
    private final Map<String, Integer> stockStorage = new ConcurrentHashMap<>();

    /**
     * 模拟已锁定库存（订单ID -> 锁定的商品和数量）
     * key: 订单ID, value: 商品ID
     */
    private final Map<String, String> lockedStock = new ConcurrentHashMap<>();

    /**
     * 初始化库存（模拟数据）
     */
    public StockService() {
        // 初始化一些商品库存
        stockStorage.put("p001", 100);  // iPhone15
        stockStorage.put("p002", 50);   // MacBook
        stockStorage.put("p003", 200);  // AirPods
    }

    /**
     * 锁定库存
     * 下单时锁定库存，防止超卖
     *
     * @param orderId   订单ID
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否锁定成功
     */
    public synchronized boolean lockStock(String orderId, String productId, int quantity) {
        Integer currentStock = stockStorage.get(productId);
        if (currentStock == null) {
            log.warn("商品不存在，商品ID: {}", productId);
            return false;
        }

        if (currentStock < quantity) {
            log.warn("库存不足，商品ID: {}, 当前库存: {}, 需要数量: {}", productId, currentStock, quantity);
            return false;
        }

        // 扣减库存
        stockStorage.put(productId, currentStock - quantity);
        // 记录锁定信息
        lockedStock.put(orderId, productId);

        log.info("库存锁定成功，订单ID: {}, 商品ID: {}, 锁定数量: {}, 剩余库存: {}",
                orderId, productId, quantity, currentStock - quantity);
        return true;
    }

    /**
     * 释放库存
     * 订单取消时释放已锁定的库存
     *
     * @param orderId  订单ID
     * @param quantity 数量
     * @return 是否释放成功
     */
    public synchronized boolean releaseStock(String orderId, int quantity) {
        String productId = lockedStock.get(orderId);
        if (productId == null) {
            log.warn("未找到订单锁定的库存，订单ID: {}", orderId);
            return false;
        }

        Integer currentStock = stockStorage.getOrDefault(productId, 0);
        stockStorage.put(productId, currentStock + quantity);
        lockedStock.remove(orderId);

        log.info("库存释放成功，订单ID: {}, 商品ID: {}, 释放数量: {}, 当前库存: {}",
                orderId, productId, quantity, currentStock + quantity);
        return true;
    }

    /**
     * 确认扣减库存
     * 订单支付成功后，确认扣减库存（移除锁定记录）
     *
     * @param orderId 订单ID
     */
    public void confirmStock(String orderId) {
        lockedStock.remove(orderId);
        log.info("库存扣减确认，订单ID: {}", orderId);
    }

    /**
     * 查询库存
     *
     * @param productId 商品ID
     * @return 库存数量
     */
    public Integer getStock(String productId) {
        return stockStorage.getOrDefault(productId, 0);
    }
}









