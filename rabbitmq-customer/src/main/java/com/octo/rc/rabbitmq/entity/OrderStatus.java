package com.octo.rc.rabbitmq.entity;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    /**
     * 待支付
     */
    PENDING_PAYMENT,
    
    /**
     * 已支付
     */
    PAID,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 已完成
     */
    COMPLETED
}





