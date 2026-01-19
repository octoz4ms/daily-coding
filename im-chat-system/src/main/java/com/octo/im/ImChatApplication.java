package com.octo.im;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IM即时通讯系统启动类
 * 
 * 核心技术点：
 * 1. WebSocket / Netty - 长连接通信
 * 2. 消息存储设计 - 分表、索引优化
 * 3. 已读未读状态 - Redis + 数据库持久化
 * 4. 群聊消息扩散 - 写扩散 vs 读扩散
 * 5. 消息可靠投递 - ACK机制 + 重试 + 离线消息
 * 
 * @author octo
 */
@SpringBootApplication
@MapperScan("com.octo.im.mapper")
@EnableAsync
@EnableScheduling
public class ImChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImChatApplication.class, args);
    }
}

