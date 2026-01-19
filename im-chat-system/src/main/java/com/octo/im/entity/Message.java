package com.octo.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息唯一ID（UUID）
     */
    private String msgId;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息类型: 1-文本 2-图片 3-语音 4-视频 5-文件 6-位置 7-撤回
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 扩展信息（JSON格式）
     */
    private String extra;

    /**
     * 状态: 0-发送中 1-已发送 2-已送达 3-已读 4-撤回
     */
    private Integer status;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    private LocalDateTime createTime;

    /**
     * 消息类型枚举
     */
    public enum MsgType {
        TEXT(1, "文本"),
        IMAGE(2, "图片"),
        VOICE(3, "语音"),
        VIDEO(4, "视频"),
        FILE(5, "文件"),
        LOCATION(6, "位置"),
        RECALL(7, "撤回");

        private final int code;
        private final String desc;

        MsgType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 消息状态枚举
     */
    public enum Status {
        SENDING(0, "发送中"),
        SENT(1, "已发送"),
        DELIVERED(2, "已送达"),
        READ(3, "已读"),
        RECALLED(4, "撤回");

        private final int code;
        private final String desc;

        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }
    }
}

