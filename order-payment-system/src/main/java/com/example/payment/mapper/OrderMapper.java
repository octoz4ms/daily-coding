package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 更新订单状态（带状态校验，防止并发问题）
     *
     * @param orderNo      订单号
     * @param newStatus    新状态
     * @param oldStatus    旧状态
     * @return 更新行数
     */
    @Update("UPDATE t_order SET status = #{newStatus}, update_time = NOW() " +
            "WHERE order_no = #{orderNo} AND status = #{oldStatus} AND deleted = 0")
    int updateStatusByOrderNo(@Param("orderNo") String orderNo,
                              @Param("newStatus") Integer newStatus,
                              @Param("oldStatus") Integer oldStatus);
}

