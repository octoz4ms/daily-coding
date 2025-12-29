# RabbitMQ 消息可靠性 Demo

本 Demo 实现了 Spring Boot + RabbitMQ 的完整消息可靠性方案。

## 可靠性机制概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          RabbitMQ 消息可靠性架构                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   Producer  │───▶│   Exchange  │───▶│    Queue    │───▶│   Consumer  │  │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘  │
│        │                  │                  │                  │          │
│        ▼                  ▼                  ▼                  ▼          │
│  ┌───────────┐      ┌───────────┐      ┌───────────┐      ┌───────────┐   │
│  │本地消息表 │      │Confirm回调│      │消息持久化 │      │手动ACK    │   │
│  │Publisher  │      │Return回调 │      │死信队列   │      │幂等性检查 │   │
│  │  Confirm  │      │           │      │           │      │重试机制   │   │
│  └───────────┘      └───────────┘      └───────────┘      └───────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 1. 生产者可靠性

### 1.1 Publisher Confirm（发布确认）
- 确保消息成功到达 Exchange
- 配置：`spring.rabbitmq.publisher-confirm-type=correlated`

### 1.2 Publisher Return（发布退回）
- 确保消息成功路由到 Queue
- 配置：`spring.rabbitmq.publisher-returns=true`

### 1.3 本地消息表
- 业务操作和消息记录在同一事务中
- 定时任务扫描重发未确认的消息
- 保证消息最终一致性

## 2. Broker 可靠性

### 2.1 交换机持久化
```java
ExchangeBuilder.directExchange("exchange").durable(true).build();
```

### 2.2 队列持久化
```java
QueueBuilder.durable("queue").build();
```

### 2.3 消息持久化
```java
MessageBuilder.withBody(body).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
```

## 3. 消费者可靠性

### 3.1 手动确认（Manual ACK）
- 配置：`spring.rabbitmq.listener.simple.acknowledge-mode=manual`
- 处理成功：`channel.basicAck()`
- 处理失败：`channel.basicNack()` 或 `channel.basicReject()`

### 3.2 消息幂等性
- 使用消息唯一ID进行去重
- 消费前检查，消费后标记

### 3.3 死信队列（DLX）
处理以下情况的消息：
- 消息被拒绝（basicNack/basicReject）且 requeue=false
- 消息TTL过期
- 队列达到最大长度

## 4. 项目结构

```
rabbitmq-customer/
├── src/main/java/com/octo/rc/rabbitmq/
│   ├── config/
│   │   └── ReliableRabbitConfig.java    # 可靠性配置
│   ├── callback/
│   │   └── RabbitConfirmCallback.java   # 生产者确认回调
│   ├── entity/
│   │   ├── MessageRecord.java           # 本地消息表实体
│   │   └── ReliableMessage.java         # 可靠消息实体
│   ├── service/
│   │   ├── MessageRecordService.java    # 本地消息表服务
│   │   ├── ReliableMessageProducer.java # 可靠消息生产者
│   │   └── IdempotentService.java       # 幂等性服务
│   ├── listener/
│   │   └── ReliableMessageConsumer.java # 可靠消息消费者
│   ├── task/
│   │   └── MessageResendTask.java       # 消息重发定时任务
│   └── controller/
│       └── ReliableMessageController.java # 测试接口
└── src/main/resources/
    └── application.yml                   # 配置文件
```

## 5. 测试接口

### 5.1 发送正常消息
```bash
curl -X POST http://localhost:8080/reliable/send \
  -H "Content-Type: application/json" \
  -d '{
    "businessId": "order-001",
    "businessType": "ORDER_CREATE",
    "data": {"orderId": "001", "amount": 100}
  }'
```

### 5.2 批量发送消息（压力测试）
```bash
curl -X POST "http://localhost:8080/reliable/send/batch?count=100"
```

### 5.3 发送会失败的消息（测试死信队列）
```bash
curl -X POST http://localhost:8080/reliable/send/error
```

### 5.4 发送可能失败的消息（测试重试机制）
```bash
curl -X POST http://localhost:8080/reliable/send/random
```

### 5.5 查询消息记录
```bash
# 查询单条消息
curl http://localhost:8080/reliable/record/{messageId}

# 查询所有消息
curl http://localhost:8080/reliable/records
```

### 5.6 查看系统状态
```bash
curl http://localhost:8080/reliable/status
```

## 6. 核心配置 (application.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    # 生产者确认
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    # 消费者配置
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 10
        retry:
          enabled: true
          initial-interval: 1s
          max-attempts: 3
        default-requeue-rejected: false
```

## 7. 流程说明

### 消息发送流程
```
1. 创建本地消息记录 (STATUS_PENDING)
2. 发送消息到 RabbitMQ
3. 更新状态为 STATUS_SENDING
4. 等待 Confirm 回调
   - 成功: 更新状态为 STATUS_SUCCESS
   - 失败: 更新状态为 STATUS_FAILED，等待重发
```

### 消息消费流程
```
1. 接收消息
2. 幂等性检查（是否已消费）
3. 业务处理
   - 成功: basicAck + 标记已消费
   - 业务异常: basicNack (不重试，转死信队列)
   - 其他异常: basicNack (重试或转死信队列)
```

### 消息重发流程
```
1. 定时任务扫描本地消息表
2. 找出待重发的消息（状态为 PENDING/SENDING 且未超过最大重试次数）
3. 使用指数退避策略计算重试间隔
4. 重发消息
```

## 8. 生产环境建议

1. **本地消息表**：使用 MySQL 存储，与业务表在同一个事务中
2. **幂等性服务**：使用 Redis 实现，支持分布式场景
3. **监控告警**：
   - 监控死信队列消息数量
   - 监控消息发送失败率
   - 监控消费者延迟
4. **消息补偿**：
   - 提供人工重发接口
   - 定期对账，发现数据不一致时补发消息

