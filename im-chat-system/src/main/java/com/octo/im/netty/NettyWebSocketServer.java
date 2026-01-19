package com.octo.im.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket服务器
 * 
 * 面试要点 - 为什么选择Netty？
 * 
 * 1. 高性能
 *    - 基于NIO，事件驱动，非阻塞IO
 *    - 零拷贝技术
 *    - 内存池化，减少GC
 * 
 * 2. 高并发
 *    - Reactor线程模型
 *    - 支持百万级连接
 * 
 * 3. 易用性
 *    - Pipeline责任链模式
 *    - 丰富的编解码器
 * 
 * Netty线程模型：
 * - BossGroup：负责接收客户端连接
 * - WorkerGroup：负责处理连接的IO读写
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer {

    @Value("${netty.websocket.port}")
    private int port;

    @Value("${netty.websocket.boss-threads}")
    private int bossThreads;

    @Value("${netty.websocket.worker-threads}")
    private int workerThreads;

    @Value("${netty.websocket.path}")
    private String wsPath;

    private final WebSocketMessageHandler webSocketMessageHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    /**
     * 启动WebSocket服务器
     */
    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                bossGroup = new NioEventLoopGroup(bossThreads);
                workerGroup = new NioEventLoopGroup(workerThreads);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        // TCP参数配置
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        // HTTP编解码器
                                        .addLast(new HttpServerCodec())
                                        // 大数据流支持
                                        .addLast(new ChunkedWriteHandler())
                                        // HTTP消息聚合
                                        .addLast(new HttpObjectAggregator(65536))
                                        // 心跳检测：读空闲60秒，写空闲30秒
                                        .addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS))
                                        // WebSocket协议处理
                                        .addLast(new WebSocketServerProtocolHandler(wsPath, null, true))
                                        // 心跳处理器
                                        .addLast(new HeartbeatHandler())
                                        // 业务消息处理器
                                        .addLast(webSocketMessageHandler);
                            }
                        });

                channelFuture = bootstrap.bind(port).sync();
                log.info("========== Netty WebSocket服务器启动成功，端口: {} ==========", port);
                
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Netty WebSocket服务器启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-server").start();
    }

    /**
     * 关闭服务器
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭Netty WebSocket服务器...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}

