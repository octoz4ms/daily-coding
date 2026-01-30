package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存Mapper
 */
@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    /**
     * 锁定库存（下单时调用）
     * 使用乐观锁防止超卖
     *
     * @param productId 商品ID
     * @param quantity  锁定数量
     * @return 更新行数
     */
    @Update("UPDATE t_stock SET available_stock = available_stock - #{quantity}, " +
            "locked_stock = locked_stock + #{quantity}, " +
            "version = version + 1, update_time = NOW() " +
            "WHERE product_id = #{productId} AND available_stock >= #{quantity}")
    int lockStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 释放库存（取消订单/超时）
     *
     * @param productId 商品ID
     * @param quantity  释放数量
     * @return 更新行数
     */
    @Update("UPDATE t_stock SET available_stock = available_stock + #{quantity}, " +
            "locked_stock = locked_stock - #{quantity}, " +
            "version = version + 1, update_time = NOW() " +
            "WHERE product_id = #{productId} AND locked_stock >= #{quantity}")
    int releaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    /**
     * 扣减库存（支付成功后调用）
     * 将锁定库存转为已售库存
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return 更新行数
     */
    @Update("UPDATE t_stock SET locked_stock = locked_stock - #{quantity}, " +
            "sold_stock = sold_stock + #{quantity}, " +
            "version = version + 1, update_time = NOW() " +
            "WHERE product_id = #{productId} AND locked_stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}

