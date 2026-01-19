package com.octo.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体（私聊/群聊）
 */
@Data
@TableName("t_conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型: 1-私聊 2-群聊
     */
    private Integer type;

    /**
     * 会话名称（群聊时为群名）
     */
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 群主ID（群聊时）
     */
    private Long ownerId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 会话类型枚举
     */
    public enum Type {
        PRIVATE(1, "私聊"),
        GROUP(2, "群聊");

        private final int code;
        private final String desc;

        Type(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }
    }
}

