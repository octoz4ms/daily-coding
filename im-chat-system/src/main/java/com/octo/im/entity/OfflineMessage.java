package com.octo.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 离线消息实体
 */
@Data
@TableName("t_offline_message")
public class OfflineMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 接收者ID
     */
    private Long userId;

    /**
     * 消息ID
     */
    private String msgId;

    /**
     * 会话ID
     */
    private Long conversationId;

    private LocalDateTime createTime;
}

