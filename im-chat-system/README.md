# IM即时通讯系统 (IM Chat System)

> 💬 基于Netty的即时通讯系统 - 面试亮点项目

## 📌 核心技术点

| 技术点 | 实现方案 | 说明 |
|-------|---------|------|
| 长连接通信 | Netty WebSocket | 高性能NIO框架 |
| 消息存储 | MySQL + 分表设计 | 支持海量消息 |
| 已读未读 | Redis + 数据库 | 快速查询 + 持久化 |
| 群聊扩散 | 写扩散策略 | 小群场景优化 |
| 消息可靠投递 | ACK + 重试 + 离线存储 | 保证不丢消息 |

## 🎯 面试必问：消息如何保证不丢失？

### 消息可靠投递机制

```
发送方                    服务端                    接收方
  │                         │                         │
  │──── 1.发送消息 ────────>│                         │
  │                         │──── 2.存储到数据库       │
  │<─── 3.服务端ACK ────────│                         │
  │                         │                         │
  │                         │──── 4.推送消息 ────────>│
  │                         │<─── 5.接收方ACK ────────│
  │                         │                         │
  │                         │──── 6.更新状态为已送达   │
  │                         │                         │
  │<─── 7.已送达通知 ───────│                         │
```

### 离线消息处理

```
接收方离线时：
┌─────────────────────────────────────────┐
│  1. 服务端检测接收方离线                  │
│  2. 消息存入离线消息表                    │
│  3. 接收方上线时触发同步                  │
│  4. 拉取并推送离线消息                    │
│  5. 删除已推送的离线记录                  │
└─────────────────────────────────────────┘
```

### 关键代码

```java
// 消息存储 + 推送
@Transactional
public void saveAndSendMessage(MessageProtocol protocol) {
    // 1. 先持久化（保证消息不丢）
    Message message = buildMessage(protocol);
    messageMapper.insert(message);

    // 2. 推送给接收方
    boolean sent = channelManager.sendToUser(receiverId, message);

    if (!sent) {
        // 3. 离线存储
        saveOfflineMessage(receiverId, protocol.getMsgId());
    }
}

// 消息ACK确认
private void handleMessageAck(MessageProtocol protocol) {
    // 更新消息状态为已送达
    messageService.updateMessageStatus(protocol.getMsgId(), DELIVERED);
}
```

## 🔄 消息状态流转

```
发送中(0) ──> 已发送(1) ──> 已送达(2) ──> 已读(3)
    │                           │
    └──────── 撤回(4) <─────────┘
```

## 📊 群聊消息扩散策略

### 写扩散 vs 读扩散

| 策略 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| 写扩散 | 读取简单，直接查个人消息 | 写放大，群大时写入多 | 小群（<500人） |
| 读扩散 | 写入少，只存一条 | 读取复杂，需聚合 | 大群、频道 |
| 混合 | 兼顾两者优点 | 实现复杂 | 微信方案 |

```java
// 本项目采用写扩散
public void broadcastGroupMessage(MessageProtocol protocol) {
    // 1. 保存一条消息
    messageMapper.insert(message);
    
    // 2. 推送给每个群成员
    for (Long memberId : memberIds) {
        boolean sent = channelManager.sendToUser(memberId, message);
        if (!sent) {
            // 离线成员存入离线表
            saveOfflineMessage(memberId, msgId);
        }
    }
}
```

## 🏗️ 系统架构

```
                    ┌─────────────────┐
                    │   客户端 APP    │
                    └────────┬────────┘
                             │ WebSocket
                             ▼
┌────────────────────────────────────────────────────┐
│                   Netty Server                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │HTTP编解码 │→│ WS协议   │→│ 心跳检测          │ │
│  └──────────┘  └──────────┘  └──────────────────┘ │
│                       │                            │
│              ┌────────▼────────┐                  │
│              │ MessageHandler  │                  │
│              └────────┬────────┘                  │
└───────────────────────┼────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌───────────┐   ┌───────────┐   ┌───────────┐
│  MySQL    │   │   Redis   │   │ RabbitMQ  │
│ 消息存储   │   │ 在线状态  │   │ 消息队列  │
└───────────┘   └───────────┘   └───────────┘
```

## 📁 项目结构

```
im-chat-system/
├── src/main/java/com/octo/im/
│   ├── ImChatApplication.java        # 启动类
│   ├── entity/
│   │   ├── User.java                 # 用户
│   │   ├── Message.java              # 消息
│   │   ├── Conversation.java         # 会话
│   │   ├── ConversationMember.java   # 会话成员
│   │   └── OfflineMessage.java       # 离线消息
│   ├── mapper/
│   │   ├── MessageMapper.java
│   │   ├── OfflineMessageMapper.java
│   │   └── ConversationMemberMapper.java
│   ├── netty/
│   │   ├── NettyWebSocketServer.java # Netty服务器
│   │   ├── ChannelManager.java       # 连接管理
│   │   ├── HeartbeatHandler.java     # 心跳处理
│   │   └── WebSocketMessageHandler.java # 消息处理
│   ├── protocol/
│   │   └── MessageProtocol.java      # 消息协议
│   └── service/
│       └── MessageService.java       # 消息服务
└── src/main/resources/
    ├── application.yml
    └── db/schema.sql
```

## 🚀 快速启动

### 1. 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+

### 2. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

### 4. WebSocket连接

```javascript
// 连接
const ws = new WebSocket('ws://localhost:9000/ws');

// 登录
ws.send(JSON.stringify({
    cmd: 1,  // CONNECT
    senderId: 10001,
    msgId: 'uuid-xxx'
}));

// 发送消息
ws.send(JSON.stringify({
    cmd: 10, // CHAT_MSG
    msgId: 'uuid-yyy',
    senderId: 10001,
    receiverId: 10002,
    conversationId: 1,
    msgType: 1,
    content: 'Hello!'
}));
```

## 🔍 面试常见追问

### 1. 为什么选择Netty而不是原生WebSocket？

**Netty优势**：
- 高性能：基于NIO，事件驱动
- 高并发：支持百万级连接
- 零拷贝：减少数据复制
- 丰富的编解码器

### 2. 如何处理消息重复？

**幂等性保证**：
- 每条消息有唯一msgId
- 数据库唯一索引
- 客户端去重展示

### 3. 心跳机制的作用？

1. **检测连接存活**：及时发现断连
2. **防止NAT超时**：保持连接活跃
3. **资源回收**：关闭无效连接

### 4. 如何支持多端同步？

```java
// 允许同一用户多端登录
Map<Long, List<Channel>> userChannels;

// 消息推送到所有端
for (Channel channel : userChannels.get(userId)) {
    channel.writeAndFlush(message);
}
```

### 5. 如何实现消息撤回？

```java
// 1. 更新消息状态为撤回
updateStatus(msgId, RECALLED);

// 2. 推送撤回通知
pushRecallNotification(msgId, receiverId);

// 3. 客户端删除本地消息
```

## 📈 性能优化建议

1. **消息分表**：按时间或会话ID分表
2. **读写分离**：从库查询历史消息
3. **消息压缩**：Protobuf序列化
4. **连接池化**：复用数据库连接
5. **本地缓存**：热点会话缓存

## 📝 License

MIT License

