package com.octo.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.octo.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 秒杀订单Mapper
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 检查用户是否已参与秒杀
     * 
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 订单数量
     */
    @Select("SELECT COUNT(1) FROM t_seckill_order WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int countByUserAndActivity(@Param("userId") Long userId, @Param("activityId") Long activityId);
}

