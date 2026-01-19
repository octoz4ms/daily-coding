package com.octo.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话成员实体
 */
@Data
@TableName("t_conversation_member")
public class ConversationMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 群内昵称
     */
    private String nickname;

    /**
     * 角色: 0-成员 1-管理员 2-群主
     */
    private Integer role;

    /**
     * 是否禁言
     */
    private Integer muted;

    /**
     * 最后已读消息ID
     */
    private Long lastReadMsgId;

    /**
     * 最后已读时间
     */
    private LocalDateTime lastReadTime;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}

