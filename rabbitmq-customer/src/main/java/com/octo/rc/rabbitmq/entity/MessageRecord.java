package com.octo.rc.rabbitmq.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地消息表实体
 * 
 * 用于实现消息的最终一致性：
 * 1. 业务操作和消息记录在同一个事务中
 * 2. 定时任务扫描未确认的消息进行重发
 * 3. 记录消息状态，便于问题追踪
 */
@Data
public class MessageRecord {

    /**
     * 消息唯一ID（用于幂等性判断）
     */
    private String messageId;

    /**
     * 业务ID（如订单ID）
     */
    private String businessId;

    /**
     * 业务类型（如 ORDER_CREATE, PAYMENT 等）
     */
    private String businessType;

    /**
     * 目标交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 消息内容（JSON格式）
     */
    private String messageBody;

    /**
     * 消息状态
     * 0: 待发送
     * 1: 已发送待确认
     * 2: 发送成功（已确认）
     * 3: 发送失败
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetryCount;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 失败原因
     */
    private String failReason;

    // 消息状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENDING = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
}

