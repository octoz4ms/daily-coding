package com.octo.im.netty;

import com.alibaba.fastjson2.JSON;
import com.octo.im.protocol.MessageProtocol;
import com.octo.im.protocol.MessageProtocol.CmdType;
import com.octo.im.service.MessageService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket消息处理器
 * 
 * 面试要点 - 消息如何保证不丢失？
 * 
 * 核心机制：ACK确认 + 重试 + 离线存储
 * 
 * 1. 发送方 -> 服务端
 *    - 发送消息后等待服务端ACK
 *    - 超时未收到ACK则重发
 *    - 消息携带唯一msgId用于去重
 * 
 * 2. 服务端 -> 接收方
 *    - 在线：直接推送，等待ACK
 *    - 离线：存入离线消息表
 *    - 上线后：拉取离线消息
 * 
 * 3. 消息状态流转
 *    发送中 -> 已发送 -> 已送达 -> 已读
 */
@Slf4j
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class WebSocketMessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ChannelManager channelManager;
    private final MessageService messageService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        log.debug("收到消息: {}", text);

        try {
            MessageProtocol protocol = JSON.parseObject(text, MessageProtocol.class);
            handleMessage(ctx, protocol);
        } catch (Exception e) {
            log.error("消息处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理不同类型的消息
     */
    private void handleMessage(ChannelHandlerContext ctx, MessageProtocol protocol) {
        CmdType cmdType = CmdType.fromCode(protocol.getCmd());
        if (cmdType == null) {
            log.warn("未知的命令类型: {}", protocol.getCmd());
            return;
        }

        switch (cmdType) {
            case CONNECT -> handleConnect(ctx, protocol);
            case HEARTBEAT -> handleHeartbeat(ctx, protocol);
            case CHAT_MSG -> handleChatMessage(ctx, protocol);
            case GROUP_MSG -> handleGroupMessage(ctx, protocol);
            case CHAT_MSG_ACK -> handleMessageAck(ctx, protocol);
            case MSG_READ -> handleMessageRead(ctx, protocol);
            case SYNC_MSG -> handleSyncMessage(ctx, protocol);
            default -> log.warn("未处理的命令类型: {}", cmdType);
        }
    }

    /**
     * 处理连接请求
     */
    private void handleConnect(ChannelHandlerContext ctx, MessageProtocol protocol) {
        Long userId = protocol.getSenderId();
        channelManager.bindUser(userId, ctx.channel());

        // 发送连接确认
        MessageProtocol ack = MessageProtocol.builder()
                .cmd(CmdType.CONNECT_ACK.getCode())
                .msgId(protocol.getMsgId())
                .timestamp(System.currentTimeMillis())
                .build();
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));

        // 推送离线消息
        messageService.pushOfflineMessages(userId);
        
        log.info("用户连接成功: userId={}", userId);
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, MessageProtocol protocol) {
        MessageProtocol ack = MessageProtocol.builder()
                .cmd(CmdType.HEARTBEAT_ACK.getCode())
                .timestamp(System.currentTimeMillis())
                .build();
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
    }

    /**
     * 处理私聊消息
     */
    private void handleChatMessage(ChannelHandlerContext ctx, MessageProtocol protocol) {
        // 1. 保存消息到数据库
        messageService.saveAndSendMessage(protocol);

        // 2. 发送ACK给发送方
        MessageProtocol ack = MessageProtocol.builder()
                .cmd(CmdType.CHAT_MSG_ACK.getCode())
                .msgId(protocol.getMsgId())
                .timestamp(System.currentTimeMillis())
                .build();
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
    }

    /**
     * 处理群聊消息
     * 
     * 面试要点 - 群聊消息扩散策略：
     * 
     * 1. 写扩散（本项目采用）
     *    - 发送时给每个群成员写一条消息
     *    - 优点：读取简单，直接查个人消息表
     *    - 缺点：写放大，群人数多时写入量大
     * 
     * 2. 读扩散
     *    - 只存一条消息到群消息表
     *    - 读取时聚合群消息和已读状态
     *    - 优点：写入少
     *    - 缺点：读取复杂，需要聚合
     * 
     * 3. 混合策略
     *    - 小群用写扩散，大群用读扩散
     *    - 微信采用此方案
     */
    private void handleGroupMessage(ChannelHandlerContext ctx, MessageProtocol protocol) {
        // 保存并群发消息
        messageService.saveAndBroadcastGroupMessage(protocol);

        // 发送ACK给发送方
        MessageProtocol ack = MessageProtocol.builder()
                .cmd(CmdType.GROUP_MSG_ACK.getCode())
                .msgId(protocol.getMsgId())
                .timestamp(System.currentTimeMillis())
                .build();
        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
    }

    /**
     * 处理消息ACK（接收方确认收到消息）
     */
    private void handleMessageAck(ChannelHandlerContext ctx, MessageProtocol protocol) {
        // 更新消息状态为已送达
        messageService.updateMessageStatus(protocol.getMsgId(), 
                com.octo.im.entity.Message.Status.DELIVERED.getCode());
    }

    /**
     * 处理已读回执
     */
    private void handleMessageRead(ChannelHandlerContext ctx, MessageProtocol protocol) {
        Long userId = channelManager.getUserId(ctx.channel());
        messageService.markAsRead(protocol.getMsgId(), userId);
    }

    /**
     * 处理消息同步请求
     */
    private void handleSyncMessage(ChannelHandlerContext ctx, MessageProtocol protocol) {
        Long userId = channelManager.getUserId(ctx.channel());
        messageService.syncMessages(userId, protocol.getTimestamp());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 用户断开连接
        channelManager.unbindUser(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket处理异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}

