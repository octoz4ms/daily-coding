package com.octo.im.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳处理器
 * 
 * 面试要点 - 为什么需要心跳？
 * 
 * 1. 检测连接是否存活
 *    - TCP KeepAlive默认2小时才检测，太久
 *    - 应用层心跳可以更快发现断连
 * 
 * 2. 防止NAT超时
 *    - 移动网络NAT通常5分钟超时
 *    - 定期心跳可以保持连接
 * 
 * 3. 资源回收
 *    - 及时关闭无效连接，释放资源
 */
@Slf4j
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    private int readIdleCount = 0;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                readIdleCount++;
                log.debug("读空闲事件: channel={}, count={}", ctx.channel().id(), readIdleCount);
                
                // 连续3次读空闲，关闭连接
                if (readIdleCount >= 3) {
                    log.warn("连接超时，关闭通道: {}", ctx.channel().id());
                    ctx.close();
                }
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲时发送心跳（这里简化处理，实际应该发送心跳包）
                log.debug("写空闲事件，发送心跳: channel={}", ctx.channel().id());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到消息，重置计数器
        readIdleCount = 0;
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("心跳处理异常: channel={}, error={}", ctx.channel().id(), cause.getMessage());
        ctx.close();
    }
}

