package com.example.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付单Mapper
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 更新支付状态（带状态校验）
     *
     * @param paymentNo  支付单号
     * @param newStatus  新状态
     * @param oldStatus  旧状态
     * @return 更新行数
     */
    @Update("UPDATE t_payment_order SET status = #{newStatus}, update_time = NOW() " +
            "WHERE payment_no = #{paymentNo} AND status = #{oldStatus} AND deleted = 0")
    int updateStatusByPaymentNo(@Param("paymentNo") String paymentNo,
                                @Param("newStatus") Integer newStatus,
                                @Param("oldStatus") Integer oldStatus);

    /**
     * 更新支付成功信息
     *
     * @param paymentNo     支付单号
     * @param transactionId 第三方交易号
     * @param callbackData  回调数据
     * @return 更新行数
     */
    @Update("UPDATE t_payment_order SET status = 2, transaction_id = #{transactionId}, " +
            "callback_data = #{callbackData}, callback_time = NOW(), pay_time = NOW(), update_time = NOW() " +
            "WHERE payment_no = #{paymentNo} AND status IN (0, 1) AND deleted = 0")
    int updatePaySuccess(@Param("paymentNo") String paymentNo,
                         @Param("transactionId") String transactionId,
                         @Param("callbackData") String callbackData);
}

