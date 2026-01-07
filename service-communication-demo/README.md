# Spring Boot 服务间通信示例

## 项目简介

本项目演示 Spring Boot 微服务间通信的两种标准规范写法：

1. **OpenFeign**（声明式，推荐）
2. **RestTemplate**（编程式，传统）

## 项目结构

```
service-communication-demo/
├── common-api/              # 公共API模块（DTO、Feign客户端接口）
├── provider-service/        # 服务提供者（用户服务，端口8081）
├── consumer-service/        # 服务消费者（订单服务，端口8082）
└── pom.xml                  # 父POM
```

## 快速启动

### 1. 编译项目

```bash
cd service-communication-demo
mvn clean install
```

### 2. 启动服务提供者

```bash
cd provider-service
mvn spring-boot:run
```

### 3. 启动服务消费者

```bash
cd consumer-service
mvn spring-boot:run
```

### 4. 测试接口

```bash
# 使用 OpenFeign 调用（推荐）
curl http://localhost:8082/api/orders/feign/1

# 使用 RestTemplate 调用
curl http://localhost:8082/api/orders/rest/1

# 直接访问用户服务
curl http://localhost:8081/api/users/1
```

---

## 服务间通信方式对比

### 方式一：OpenFeign（推荐）

#### 核心代码

```java
// 1. 定义 Feign 客户端接口
@FeignClient(
    name = "provider-service",
    url = "${provider.service.url:http://localhost:8081}",
    path = "/api/users",
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {
    @GetMapping("/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);
}

// 2. 启动类启用 Feign
@EnableFeignClients(basePackages = "com.octo.demo.common.client")

// 3. 注入使用
@Autowired
private UserClient userClient;

Result<UserDTO> result = userClient.getUserById(1L);
```

#### 优点

| 特性 | 说明 |
|------|------|
| 声明式调用 | 接口定义清晰，代码简洁 |
| 自动序列化 | 自动处理 JSON 序列化/反序列化 |
| 降级支持 | 内置 Fallback 机制 |
| 可配置性 | 支持超时、重试、日志等配置 |
| 负载均衡 | 配合注册中心自动负载均衡 |

#### 最佳实践

1. **Feign 接口定义放在独立的 common-api 模块**
2. **使用 FallbackFactory 而非 Fallback**（可获取异常信息）
3. **配置合理的超时时间**
4. **开启日志便于调试**

---

### 方式二：RestTemplate（传统）

#### 核心代码

```java
// 1. 配置 RestTemplate Bean
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(5))
        .build();
}

// 2. 注入使用
@Autowired
private RestTemplate restTemplate;

String url = "http://localhost:8081/api/users/" + userId;
ResponseEntity<Result<UserDTO>> response = restTemplate.exchange(
    url,
    HttpMethod.GET,
    null,
    new ParameterizedTypeReference<Result<UserDTO>>() {}
);
```

#### 优点

| 特性 | 说明 |
|------|------|
| 灵活性高 | 可处理复杂请求场景 |
| 无额外依赖 | Spring Web 内置组件 |
| 完全控制 | 可自定义请求头、拦截器等 |

#### 缺点

| 问题 | 说明 |
|------|------|
| 代码冗长 | 需要手动构造 URL、处理响应 |
| 易出错 | 手动处理异常和超时 |
| 维护成本高 | 接口变更需要修改多处 |

---

## 生产环境推荐方案

### 不依赖注册中心（简单部署）

```yaml
# application.yml
provider:
  service:
    url: http://provider-host:8081
```

```java
@FeignClient(
    name = "provider-service",
    url = "${provider.service.url}"
)
```

### 使用注册中心（生产推荐）

```java
// 移除 url 属性，通过服务名发现
@FeignClient(name = "provider-service")
public interface UserClient {
    // ...
}
```

支持的注册中心：
- **Nacos**（阿里云，推荐）
- **Consul**
- **Eureka**
- **Zookeeper**

---

## 配置说明

### Feign 配置项

```yaml
spring.cloud.openfeign:
  # 熔断器
  circuitbreaker:
    enabled: true
  # 客户端配置
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: BASIC
```

### Feign 日志级别

| 级别 | 说明 |
|------|------|
| NONE | 不记录日志（默认） |
| BASIC | 记录请求方法、URL、响应状态码和执行时间 |
| HEADERS | BASIC + 请求和响应头 |
| FULL | 完整的请求和响应（包含body） |

---

## 目录结构详解

```
common-api/
├── src/main/java/com/octo/demo/common/
│   ├── dto/
│   │   ├── Result.java          # 统一响应封装
│   │   ├── UserDTO.java         # 用户DTO
│   │   └── OrderDTO.java        # 订单DTO
│   └── client/
│       ├── UserClient.java              # Feign客户端接口
│       └── UserClientFallbackFactory.java  # 降级工厂

provider-service/
├── src/main/java/com/octo/demo/provider/
│   ├── ProviderServiceApplication.java
│   ├── controller/
│   │   └── UserController.java
│   └── service/
│       ├── UserService.java
│       └── impl/UserServiceImpl.java

consumer-service/
├── src/main/java/com/octo/demo/consumer/
│   ├── ConsumerServiceApplication.java
│   ├── config/
│   │   ├── FeignConfig.java         # Feign配置
│   │   └── RestTemplateConfig.java  # RestTemplate配置
│   ├── controller/
│   │   └── OrderController.java
│   └── service/
│       ├── OrderService.java
│       └── impl/OrderServiceImpl.java
```

---

## 总结

| 方式 | 推荐场景 | 复杂度 |
|------|---------|--------|
| **OpenFeign** | 标准的服务间调用 | ⭐ 简单 |
| **RestTemplate** | 特殊需求、遗留系统 | ⭐⭐ 中等 |
| **WebClient** | 响应式编程 | ⭐⭐⭐ 较高 |

**推荐使用 OpenFeign**：声明式、易维护、功能完善。

