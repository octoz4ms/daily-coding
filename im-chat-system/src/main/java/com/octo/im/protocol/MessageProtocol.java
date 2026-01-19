package com.octo.im.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket消息协议
 * 
 * 统一的消息格式，用于客户端和服务端通信
 * 
 * 消息可靠投递机制（面试重点）：
 * 
 * 1. 客户端发送消息 -> 服务端收到后存储到数据库 -> 返回ACK给发送方
 * 2. 服务端推送消息给接收方 -> 接收方收到后返回ACK -> 服务端标记为已送达
 * 3. 如果没收到ACK，服务端会重试推送
 * 4. 如果接收方离线，消息存入离线消息表，上线后拉取
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageProtocol implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息命令类型
     */
    private Integer cmd;

    /**
     * 消息唯一ID（用于ACK确认）
     */
    private String msgId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID（私聊时）
     */
    private Long receiverId;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息类型: 1-文本 2-图片 3-语音等
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 扩展信息
     */
    private String extra;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 命令类型枚举
     */
    public enum CmdType {
        // 连接相关
        CONNECT(1, "连接"),
        CONNECT_ACK(2, "连接确认"),
        DISCONNECT(3, "断开连接"),
        HEARTBEAT(4, "心跳"),
        HEARTBEAT_ACK(5, "心跳确认"),

        // 消息相关
        CHAT_MSG(10, "聊天消息"),
        CHAT_MSG_ACK(11, "消息确认"),
        MSG_DELIVERED(12, "消息已送达"),
        MSG_READ(13, "消息已读"),

        // 群聊相关
        GROUP_MSG(20, "群消息"),
        GROUP_MSG_ACK(21, "群消息确认"),

        // 通知相关
        NOTIFICATION(30, "通知"),
        RECALL_MSG(31, "撤回消息"),

        // 同步相关
        SYNC_MSG(40, "同步消息"),
        SYNC_READ_STATUS(41, "同步已读状态");

        private final int code;
        private final String desc;

        CmdType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public static CmdType fromCode(int code) {
            for (CmdType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }
}

