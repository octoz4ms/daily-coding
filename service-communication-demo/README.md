# Spring Boot 服务间通信示例

## 项目简介

本项目演示 Spring Boot 服务间几种常见调用方式，并重点说明：

1. **OpenFeign**：声明式 HTTP 调用，微服务里最常见
2. **RestTemplate**：传统同步 HTTP 调用
3. **WebClient**：响应式 HTTP 调用
4. **MQ**：异步消息通信（本项目提供概念说明，不在本模块落地中间件）

同时覆盖两个实际部署场景：

- **本地联调**：两个服务都跑在同一台机器上，通过 `localhost + 端口` 调用
- **跨服务器部署**：两个服务部署在不同机器上，通过 `IP/域名/网关地址` 调用

## 项目结构

```text
service-communication-demo/
├── common-api/              # 公共API模块（DTO、Feign客户端接口）
├── provider-service/        # 服务提供者（用户服务，端口8081）
├── consumer-service/        # 服务消费者（订单服务，端口8082）
└── pom.xml                  # 父POM
```

## 这个 demo 能学到什么

- 服务间最常见的调用方式有哪些
- 本地调用和跨服务器调用，本质上有什么相同点和不同点
- 为什么开发环境常写 `localhost`，而生产环境不能这么写
- 为什么跨服务器调用更依赖超时、重试、熔断、网关、注册中心

## 快速启动

### 1. 编译项目

```bash
mvn clean install
```

### 2. 启动 provider-service

```bash
mvn -pl provider-service spring-boot:run
```

### 3. 启动 consumer-service

```bash
mvn -pl consumer-service spring-boot:run
```

## 测试接口

### 1. 直接访问 provider

```bash
curl http://localhost:8081/api/users/1
```

### 2. 三种同步 HTTP 调用方式

```bash
curl http://localhost:8082/api/orders/feign/1
curl http://localhost:8082/api/orders/rest/1
curl http://localhost:8082/api/orders/webclient/1
```

### 3. 查看调用方式对比

```bash
curl http://localhost:8082/api/orders/communication-methods
```

### 4. 查看本地部署与跨服务器部署差异

```bash
curl http://localhost:8082/api/orders/deployment-differences
```

## 服务间调用方式有哪些

### 1. HTTP 同步调用

最常见，调用方发送请求，等待响应。

#### OpenFeign

- 本质：还是 HTTP
- 特点：像调用本地接口一样调用远程服务
- 适合：标准微服务项目

#### RestTemplate

- 本质：同步 HTTP 客户端
- 特点：自己拼 URL、自己处理请求和响应
- 适合：老项目、简单项目、想完全控制请求细节

#### WebClient

- 本质：响应式 HTTP 客户端
- 特点：支持异步、非阻塞
- 适合：高并发、响应式项目

### 2. RPC 调用

典型如 Dubbo、gRPC。

- 优点：性能高、接口契约清晰
- 缺点：接入成本比 HTTP 更高
- 适合：内部服务高频调用、对性能敏感的系统

### 3. MQ 异步调用

典型如 RabbitMQ、Kafka、RocketMQ。

- 优点：解耦、削峰、异步处理
- 缺点：不是“发出去立刻拿结果”的调用方式
- 适合：下单后发消息、支付回调、库存扣减、通知发送

## 本地部署和跨服务器部署的区别

### 相同点

- 本质都是一个服务通过网络协议调用另一个服务
- 都需要知道目标地址
- 都要考虑超时、失败、重试、异常处理

### 不同点

#### 1. 地址不同

本地联调通常这样写：

```yaml
provider:
  service:
    url: http://localhost:8081
```

跨服务器部署通常这样写：

```yaml
provider:
  service:
    url: http://192.168.1.20:8081
```

或者：

```yaml
provider:
  service:
    url: http://user-service.internal
```

或者通过网关：

```yaml
provider:
  service:
    url: http://gateway.company.com/user-service
```

#### 2. 网络问题复杂度不同

本地联调时，主要问题通常是：

- 服务没启动
- 端口写错
- 接口路径写错

跨服务器部署时，还要额外考虑：

- 防火墙是否放行
- 安全组是否开放
- DNS 是否可解析
- 目标 IP 是否可达
- HTTPS 证书是否正确
- 网关路由是否配置正确
- 延迟、丢包、抖动是否可接受

#### 3. 治理方式不同

本地联调可以临时写死地址。

跨服务器部署更推荐：

- 配置中心
- 注册中心
- 网关
- 负载均衡
- 熔断降级
- 链路追踪

## 本项目的关键配置

consumer-service 中：

```yaml
provider:
  service:
    url: ${PROVIDER_SERVICE_URL:http://localhost:8081}

deployment:
  mode: ${DEPLOYMENT_MODE:local}
```

含义：

- 默认本地联调，用 `http://localhost:8081`
- 如果部署到不同服务器，只需要在启动时覆盖环境变量即可

例如：

```bash
set PROVIDER_SERVICE_URL=http://192.168.1.20:8081
set DEPLOYMENT_MODE=remote
mvn -pl consumer-service spring-boot:run
```

## 推荐怎么选

### 开发学习阶段

- `RestTemplate`：便于理解原理
- `OpenFeign`：便于写业务代码

### 企业项目里

- 同步调用优先考虑 `OpenFeign`
- 高并发响应式项目考虑 `WebClient`
- 需要解耦和异步时使用 `MQ`
- 对性能和接口契约要求更高时考虑 `gRPC / Dubbo`

## 总结

一句话总结：

- **本地调用**：通常是 `localhost + 端口`
- **跨服务器调用**：通常是 `IP / 域名 / 网关地址`
- **调用方式**：可以是 HTTP、RPC、MQ
- **业务里最常见**：`OpenFeign + 网关/注册中心`

如果你愿意，我下一步还可以继续帮你把这个项目再扩展成：

1. **加入 Nacos 注册中心版本**
2. **加入 RabbitMQ 异步调用 demo**
3. **加入 gRPC demo**

这样你就能把“同步调用 / 异步调用 / 本地部署 / 分布式部署”一套全学完。

