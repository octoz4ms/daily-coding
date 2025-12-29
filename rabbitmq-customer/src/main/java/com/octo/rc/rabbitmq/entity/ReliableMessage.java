package com.octo.rc.rabbitmq.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可靠消息实体
 * 
 * 包含消息ID用于幂等性判断
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReliableMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID（UUID）
     */
    private String messageId;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 消息内容
     */
    private Object data;

    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;

    /**
     * 时间戳（用于消息过期判断）
     */
    private Long timestamp;
}

