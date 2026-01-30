# 支付系统 (Payment System)

这是一个集成微信支付和支付宝的Spring Boot支付系统。

## 功能特性

- 支持微信支付和支付宝
- 支付订单管理
- 支付回调处理
- 退款功能
- Redis缓存支持

## 技术栈

- Spring Boot 2.7.18
- Java 8
- Redis
- 微信支付Java SDK
- 支付宝Java SDK
- Lombok
- Maven

## 项目结构

```
payment-system/
├── src/
│   ├── main/
│   │   ├── java/com/octo/payment/
│   │   │   ├── PaymentSystemApplication.java          # 启动类
│   │   │   ├── config/                                # 配置类
│   │   │   │   ├── WeChatPayConfig.java              # 微信支付配置
│   │   │   │   └── AlipayConfig.java                 # 支付宝配置
│   │   │   ├── controller/                            # 控制器
│   │   │   │   ├── PaymentController.java            # 支付接口
│   │   │   │   └── CallbackController.java           # 回调处理
│   │   │   ├── dto/                                  # 数据传输对象
│   │   │   │   ├── PaymentRequest.java               # 支付请求
│   │   │   │   ├── PaymentResponse.java              # 支付响应
│   │   │   │   ├── RefundRequest.java                # 退款请求
│   │   │   │   └── PaymentCallback.java              # 回调数据
│   │   │   ├── entity/                               # 实体类
│   │   │   │   └── PaymentOrder.java                 # 支付订单
│   │   │   ├── enums/                                # 枚举类
│   │   │   │   ├── PaymentMethod.java                # 支付方式
│   │   │   │   └── PaymentStatus.java                # 支付状态
│   │   │   ├── service/                              # 服务层
│   │   │   │   ├── PaymentService.java               # 支付服务接口
│   │   │   │   └── impl/                             # 服务实现
│   │   │   │       ├── PaymentServiceImpl.java       # 支付服务实现
│   │   │   │       ├── WeChatPayService.java         # 微信支付服务
│   │   │   │       └── AlipayService.java            # 支付宝服务
│   │   └── resources/
│   │       └── application.yml                       # 配置文件
│   └── test/
│       └── java/com/octo/payment/
│           └── PaymentSystemApplicationTests.java    # 测试类
├── pom.xml                                           # Maven配置
└── README.md                                         # 说明文档
```

## 配置说明

### 微信支付配置

在 `application.yml` 中配置微信支付相关参数：

```yaml
wechat:
  pay:
    app-id: your_wechat_app_id
    mch-id: your_merchant_id
    private-key-path: classpath:cert/apiclient_key.pem
    merchant-serial-number: your_merchant_serial_number
    api-v3-private-key: your_api_v3_private_key
    notify-url: http://your-domain.com/api/payment/wechat/callback
```

### 支付宝配置

```yaml
alipay:
  app-id: your_alipay_app_id
  private-key: your_private_key
  public-key: your_public_key
  notify-url: http://your-domain.com/api/payment/alipay/callback
  return-url: http://your-domain.com/payment/success
  gateway-url: https://openapi.alipay.com/gateway.do
```

## API接口

### 创建支付订单

```
POST /api/payment/create
```

请求体：
```json
{
  "merchantOrderId": "ORDER123456",
  "userId": "USER001",
  "amount": 99.99,
  "paymentMethod": "WECHAT", // 或 "ALIPAY"
  "description": "商品购买",
  "notifyUrl": "http://your-domain.com/notify",
  "returnUrl": "http://your-domain.com/success",
  "attach": "附加数据"
}
```

### 查询支付订单

```
GET /api/payment/query/{orderId}
```

### 关闭支付订单

```
POST /api/payment/close/{orderId}
```

### 申请退款

```
POST /api/payment/refund
```

请求体：
```json
{
  "originalOrderId": "PAY123456789",
  "refundOrderId": "REFUND123456",
  "refundAmount": 99.99,
  "refundReason": "用户取消"
}
```

## 支付流程

1. 前端调用创建支付订单接口
2. 后端根据支付方式调用微信或支付宝API
3. 返回支付参数（微信返回二维码URL，支付宝返回支付URL）
4. 用户完成支付
5. 支付平台回调通知支付结果
6. 系统更新订单状态并执行业务逻辑

## 运行项目

1. 安装依赖：
```bash
mvn clean install
```

2. 启动应用：
```bash
mvn spring-boot:run
```

3. 访问接口：
```
http://localhost:8080
```

## 注意事项

1. 生产环境需要将订单存储到数据库而不是Redis
2. 需要正确配置支付平台的回调URL
3. 需要实现回调签名的验证逻辑
4. 需要处理支付超时和失败的情况
5. 建议添加支付日志记录和监控

## 开发计划

- [ ] 添加数据库支持（MySQL/PostgreSQL）
- [ ] 完善回调签名验证
- [ ] 添加支付超时处理
- [ ] 实现完整的退款流程
- [ ] 添加支付统计功能
- [ ] 支持分账功能
- [ ] 添加单元测试和集成测试
