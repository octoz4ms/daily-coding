package com.octo.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀活动Mapper
 */
@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 扣减库存 - 使用乐观锁防止超卖
     * 
     * 面试重点：这里使用 available_stock > 0 作为条件，配合乐观锁version，
     * 即使并发情况下也能保证库存不会变成负数
     *
     * @param activityId 活动ID
     * @return 影响行数，0表示扣减失败（库存不足或版本冲突）
     */
    @Update("UPDATE t_seckill_activity SET available_stock = available_stock - 1, " +
            "version = version + 1 " +
            "WHERE id = #{activityId} AND available_stock > 0")
    int deductStock(@Param("activityId") Long activityId);

    /**
     * 恢复库存 - 订单取消或超时时调用
     *
     * @param activityId 活动ID
     * @return 影响行数
     */
    @Update("UPDATE t_seckill_activity SET available_stock = available_stock + 1, " +
            "version = version + 1 " +
            "WHERE id = #{activityId}")
    int restoreStock(@Param("activityId") Long activityId);
}

