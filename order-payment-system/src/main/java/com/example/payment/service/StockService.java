package com.example.payment.service;

/**
 * 库存服务接口
 */
public interface StockService {

    /**
     * 检查库存是否充足
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否充足
     */
    boolean checkStock(Long productId, Integer quantity);

    /**
     * 锁定库存（下单时调用）
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否成功
     */
    boolean lockStock(Long productId, Integer quantity);

    /**
     * 释放库存（取消订单/超时）
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否成功
     */
    boolean releaseStock(Long productId, Integer quantity);

    /**
     * 扣减库存（支付成功后）
     *
     * @param productId 商品ID
     * @param quantity  数量
     * @return 是否成功
     */
    boolean deductStock(Long productId, Integer quantity);

    /**
     * 获取可用库存
     *
     * @param productId 商品ID
     * @return 可用库存数量
     */
    Integer getAvailableStock(Long productId);
}

