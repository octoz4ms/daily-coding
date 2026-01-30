# 订单支付系统 (Order Payment System)

一个完整的下单支付系统示例，实现了大厂级别的订单支付流程，包括订单创建、库存管理、支付处理、订单超时自动取消等功能。

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              系统架构图                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐          │
│  │  前端     │ →  │ 订单服务  │ →  │ 支付服务  │ →  │ 第三方支付 │          │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘          │
│                       ↓               ↓                                  │
│                  ┌──────────┐    ┌──────────┐                           │
│                  │ 库存服务  │    │ RabbitMQ │                           │
│                  └──────────┘    └──────────┘                           │
│                       ↓               ↓                                  │
│                  ┌──────────┐    ┌──────────┐                           │
│                  │  Redis   │    │  MySQL   │                           │
│                  └──────────┘    └──────────┘                           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## ✨ 功能特性

### 订单模块
- ✅ 防重复提交（Token令牌机制）
- ✅ 商品库存校验
- ✅ 库存预扣（Redis + MySQL乐观锁）
- ✅ 订单创建
- ✅ 订单超时自动取消（RabbitMQ延迟队列）
- ✅ 订单状态管理

### 支付模块
- ✅ 支付单创建
- ✅ 微信支付对接（模拟）
- ✅ 支付宝对接（模拟）
- ✅ 支付回调处理
- ✅ 回调幂等性保证
- ✅ 支付成功通知（MQ异步）

### 库存模块
- ✅ Redis缓存库存
- ✅ 库存锁定/释放/扣减
- ✅ 乐观锁防止超卖

## 🔄 核心流程

### 下单流程
```
1. 获取提交令牌 → 防重复提交
2. 验证商品和库存
3. 锁定库存（Redis + DB）
4. 创建订单（状态：待支付）
5. 发送延迟消息（订单超时）
6. 返回订单信息
```

### 支付流程
```
1. 创建支付单
2. 调用第三方支付预下单
3. 返回支付参数给前端
4. 用户完成支付
5. 支付平台异步回调
6. 验签 + 幂等处理
7. 更新支付单状态
8. 发送MQ通知订单服务
9. 订单服务更新订单状态 + 扣减库存
```

### 超时取消流程
```
1. 订单创建时发送延迟消息（30分钟）
2. 消息到期，消费者接收
3. 检查订单状态（是否已支付）
4. 未支付则关闭订单 + 释放库存
```

## 🛠️ 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.x | 基础框架 |
| MyBatis Plus 3.5.x | ORM框架 |
| MySQL 8.0 | 数据库 |
| Redis | 缓存、分布式锁、库存缓存 |
| RabbitMQ | 消息队列（延迟队列、异步通知） |
| Lombok | 简化代码 |
| Hutool | 工具类库 |

## 📁 项目结构

```
order-payment-system/
├── src/main/java/com/example/payment/
│   ├── config/          # 配置类
│   │   ├── RabbitMQConfig.java
│   │   └── MyBatisPlusConfig.java
│   ├── controller/      # 控制器
│   │   ├── OrderController.java
│   │   └── PaymentController.java
│   ├── dto/             # 数据传输对象
│   │   ├── request/
│   │   └── response/
│   ├── entity/          # 实体类
│   │   ├── Order.java
│   │   ├── PaymentOrder.java
│   │   ├── Product.java
│   │   └── Stock.java
│   ├── enums/           # 枚举
│   │   ├── OrderStatus.java
│   │   ├── PaymentStatus.java
│   │   └── PaymentMethod.java
│   ├── exception/       # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── mapper/          # MyBatis Mapper
│   ├── mq/              # 消息队列
│   │   ├── OrderMessageProducer.java
│   │   ├── PaymentMessageProducer.java
│   │   ├── OrderTimeoutConsumer.java
│   │   └── PaymentSuccessConsumer.java
│   ├── service/         # 服务层
│   │   ├── OrderService.java
│   │   ├── PaymentService.java
│   │   ├── StockService.java
│   │   └── impl/
│   └── OrderPaymentApplication.java
├── src/main/resources/
│   ├── application.yml  # 配置文件
│   └── schema.sql       # 数据库初始化脚本
├── pom.xml
└── README.md
```

## 🚀 快速开始

### 1. 环境要求

- JDK 11+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

### 2. 初始化数据库

```bash
# 登录MySQL，执行初始化脚本
mysql -u root -p < src/main/resources/sql/init.sql
```

或者手动执行 `src/main/resources/sql/init.sql` 中的SQL语句。

### 3. 修改数据库配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_payment?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root      # 修改为你的用户名
    password: root      # 修改为你的密码
```

### 4. 启动依赖服务

```bash
# 启动 MySQL
# (Windows) net start mysql
# (Linux) sudo systemctl start mysqld

# 启动 Redis
redis-server

# 启动 RabbitMQ
rabbitmq-server
```

### 5. 运行项目

```bash
cd order-payment-system

# 打包
mvn clean package -DskipTests

# 运行
java -jar target/order-payment-system-1.0.0.jar

# 或者直接运行
mvn spring-boot:run
```

### 6. 访问服务

- 应用地址: http://localhost:8080

## 📡 API接口

### 订单接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/order/submit-token` | GET | 获取提交令牌 |
| `/api/order/create` | POST | 创建订单 |
| `/api/order/detail/{orderNo}` | GET | 查询订单详情 |
| `/api/order/cancel/{orderNo}` | POST | 取消订单 |

### 支付接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/payment/create` | POST | 创建支付单 |
| `/api/payment/query/{paymentNo}` | GET | 查询支付单 |
| `/api/payment/query/order/{orderNo}` | GET | 根据订单号查询支付单 |
| `/api/payment/close/{paymentNo}` | POST | 关闭支付单 |
| `/api/payment/callback/wechat` | POST | 微信支付回调 |
| `/api/payment/callback/alipay` | POST | 支付宝回调 |

## 📝 接口示例

### 1. 获取提交令牌

```bash
curl "http://localhost:8080/api/order/submit-token?userId=1"
```

响应:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": "a1b2c3d4e5f6"
}
```

### 2. 创建订单

```bash
curl -X POST "http://localhost:8080/api/order/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 1,
    "receiverName": "张三",
    "receiverPhone": "13800138000",
    "receiverAddress": "北京市朝阳区xxx",
    "submitToken": "a1b2c3d4e5f6"
  }'
```

响应:
```json
{
  "code": 200,
  "message": "订单创建成功",
  "data": {
    "orderNo": "ORD20260130120000ABCDEF",
    "productName": "iPhone 15 Pro",
    "quantity": 1,
    "payAmount": 8999.00,
    "status": 0,
    "statusDesc": "待支付",
    "remainingPayTime": 1800
  }
}
```

### 3. 发起支付

```bash
curl -X POST "http://localhost:8080/api/payment/create" \
  -H "Content-Type: application/json" \
  -d '{
    "orderNo": "ORD20260130120000ABCDEF",
    "paymentMethod": 1,
    "returnUrl": "http://example.com/success"
  }'
```

响应:
```json
{
  "code": 200,
  "message": "支付单创建成功",
  "data": {
    "paymentNo": "PAY20260130120001GHIJKL",
    "orderNo": "ORD20260130120000ABCDEF",
    "amount": 8999.00,
    "paymentMethod": 1,
    "paymentMethodDesc": "微信支付",
    "status": 1,
    "statusDesc": "支付中",
    "payUrl": "weixin://wxpay/bizpayurl?pr=xxx"
  }
}
```

## ⚙️ 配置说明

### application.yml 主要配置

```yaml
# 订单配置
order:
  timeout-minutes: 30    # 订单超时时间（分钟）
  prefix: "ORD"          # 订单号前缀

# 支付配置
payment:
  timeout-minutes: 30    # 支付单超时时间
  prefix: "PAY"          # 支付单号前缀
  
  # 微信支付配置
  wechat:
    app-id: your_app_id
    mch-id: your_mch_id
    api-key: your_api_key
    notify-url: http://your-domain.com/api/payment/callback/wechat
  
  # 支付宝配置
  alipay:
    app-id: your_app_id
    private-key: your_private_key
    public-key: alipay_public_key
    notify-url: http://your-domain.com/api/payment/callback/alipay
```

## 🔒 关键设计

### 1. 防重复提交

使用Token令牌机制，创建订单前先获取令牌，提交时携带令牌，后端验证并消费令牌。

### 2. 库存扣减

采用"预扣库存"方案：
- 下单时：锁定库存（可用库存减少，锁定库存增加）
- 支付成功：扣减库存（锁定库存转为已售库存）
- 取消/超时：释放库存（锁定库存还原为可用库存）

### 3. 订单超时

使用RabbitMQ死信队列实现延迟消息，订单创建时发送延迟消息，到期后自动处理超时逻辑。

### 4. 回调幂等

使用Redis SETNX实现幂等，确保同一笔支付回调只处理一次。

## 📄 License

MIT License

