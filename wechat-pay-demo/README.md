# 微信支付 Java 标准流程示例

基于微信支付 APIv3 官方 Java SDK 实现的规范支付流程，包含 Native 支付、支付回调验签、订单查询、关闭订单。

## 技术栈

- Java 17
- Spring Boot 3.2
- 微信支付 wechatpay-java 0.2.17

## 支付流程

```
┌─────────┐    ① 下单请求     ┌─────────────┐    ② 返回 code_url    ┌─────────┐
│  商户   │ ───────────────> │  本服务     │ <─────────────────── │  商户   │
│  后台   │                  │  (后端)     │                      │  后台   │
└─────────┘                  └─────────────┘                      └─────────┘
                                    │
                                    │ ③ 生成二维码展示
                                    ▼
                              ┌─────────┐
                              │  用户    │ ④ 微信扫码支付
                              └─────────┘
                                    │
                                    │ ⑤ 支付成功
                                    ▼
┌─────────┐    ⑥ 异步通知     ┌─────────────┐
│  微信   │ ───────────────> │  本服务     │ 验签、解密、更新订单
│  支付   │   (POST /notify)  │  (回调)     │
└─────────┘                  └─────────────┘
```

## 前置准备

1. **成为微信支付商户**：https://pay.weixin.qq.com/
2. **获取商户证书**：商户平台 → API 安全 → 申请 API 证书，下载后得到 `apiclient_key.pem`
3. **获取证书序列号**：商户平台 → API 安全 → API 证书 → 查看证书序列号
4. **设置 APIv3 密钥**：商户平台 → API 安全 → 设置 APIv3 密钥（32 位）

## 配置

编辑 `application.yml` 或使用环境变量：

```yaml
wechat:
  pay:
    app-id: wxa9d9651ae******      # 公众号/小程序/APP 的 AppID
    mch-id: 190000****             # 商户号
    private-key-path: classpath:cert/apiclient_key.pem  # 商户私钥路径
    merchant-serial-number: 5157F09EFDC096DE15EBE81A47057A72********
    api-v3-key: your_32_char_api_v3_key
    notify-url: https://your-domain.com/api/wechat/pay/notify  # 支付回调 URL，需公网可访问
```

将 `apiclient_key.pem` 放入 `src/main/resources/cert/` 目录。

## API 接口

### 1. Native 下单

```bash
POST /api/wechat/pay/native/prepay
Content-Type: application/json

{
  "outTradeNo": "ORDER202402270001",
  "description": "测试商品",
  "totalAmount": 0.01,
  "attach": "自定义数据"
}
```

响应：
```json
{
  "outTradeNo": "ORDER202402270001",
  "codeUrl": "weixin://wxpay/bizpayurl?pr=xxx",
  "prepayId": null
}
```

将 `codeUrl` 转为二维码，用户微信扫码即可支付。

### 2. 查询订单

```bash
GET /api/wechat/pay/order/{outTradeNo}
```

### 3. 关闭订单

```bash
POST /api/wechat/pay/order/{outTradeNo}/close
```

### 4. 支付回调（微信服务器调用）

```
POST /api/wechat/pay/notify
```

回调会自动验签、解密，业务逻辑在 `WeChatPayNotifyController.handlePaySuccess()` 中实现。

## 本地调试

支付回调需公网 URL，本地开发可使用：

- [ngrok](https://ngrok.com/)：`ngrok http 8080`
- [内网穿透](https://natapp.cn/)

将生成的公网地址配置为 `notify-url`。

## 运行

```bash
mvn spring-boot:run
```

## 项目结构

```
wechat-pay-demo/
├── config/           # 微信支付配置
├── controller/       # REST 控制器
├── dto/              # 请求/响应 DTO
├── service/          # 支付业务服务
└── resources/
    └── cert/         # 商户证书目录
```

## 参考文档

- [微信支付商户平台](https://pay.weixin.qq.com/)
- [Native 支付开发文档](https://pay.weixin.qq.com/docs/merchant/products/native-payment/introduction.html)
- [wechatpay-java SDK](https://github.com/wechatpay-apiv3/wechatpay-java)
