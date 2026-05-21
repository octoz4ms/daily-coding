# Spring Cloud 微服务示例项目

## 项目简介

本项目是一个基于 Spring Cloud + Spring Cloud Alibaba 的微服务示例项目，演示了微服务架构的核心组件使用方式。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 基础框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 阿里巴巴微服务组件 |
| Nacos | 2.x | 服务注册中心 + 配置中心 |
| Gateway | - | API 网关 |
| OpenFeign | - | 服务间调用 |
| Sentinel | - | 熔断限流 |

## 项目结构

```
springcloud-demo/
├── pom.xml                    # 父工程 POM
├── common/                    # 公共模块
│   └── src/main/java/com/example/common/
│       ├── entity/            # 实体类
│       └── result/            # 统一响应结果
├── gateway/                   # API 网关服务 (端口: 8080)
│   └── src/main/java/com/example/gateway/
├── user-service/              # 用户服务 (端口: 8081)
│   └── src/main/java/com/example/user/
│       └── controller/        # 用户控制器
└── order-service/             # 订单服务 (端口: 8082)
    └── src/main/java/com/example/order/
        ├── controller/        # 订单控制器
        └── feign/             # Feign 客户端
```

## 环境准备

### 1. 安装 Nacos

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.tar.gz

# 解压
tar -xzf nacos-server-2.3.0.tar.gz

# 进入目录
cd nacos/bin

# 单机模式启动
sh startup.sh -m standalone
```

访问 Nacos 控制台：http://localhost:8848/nacos  
默认账号密码：nacos/nacos

### 2. 安装 Sentinel Dashboard（可选）

```bash
# 下载 Sentinel Dashboard
wget https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar

# 启动
java -Dserver.port=8080 -jar sentinel-dashboard-1.8.6.jar
```

访问 Sentinel 控制台：http://localhost:8080  
默认账号密码：sentinel/sentinel

## 启动步骤

### 1. 克隆项目并构建

```bash
# 进入项目目录
cd springcloud-demo

# Maven 构建
mvn clean install -DskipTests
```

### 2. 启动服务（按顺序）

```bash
# 1. 启动用户服务
cd user-service
mvn spring-boot:run

# 2. 启动订单服务（新终端）
cd order-service
mvn spring-boot:run

# 3. 启动网关服务（新终端）
cd gateway
mvn spring-boot:run
```

## 接口测试

### 直接访问服务

```bash
# 获取用户信息
curl http://localhost:8081/user/1

# 获取订单信息
curl http://localhost:8082/order/1

# 获取订单及用户信息（演示服务间调用）
curl http://localhost:8082/order/with-user/1
```

### 通过网关访问

```bash
# 通过网关获取用户信息
curl http://localhost:8080/api/user/user/1

# 通过网关获取订单信息
curl http://localhost:8080/api/order/order/1
```

## 核心功能演示

### 1. 服务注册与发现

启动服务后，访问 Nacos 控制台，可以在「服务管理 -> 服务列表」中看到已注册的服务：
- gateway-service
- user-service
- order-service

### 2. 服务间调用（OpenFeign）

订单服务通过 Feign 调用用户服务获取用户信息：

```java
@FeignClient(value = "user-service", path = "/user")
public interface UserFeignClient {
    @GetMapping("/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);
}
```

### 3. API 网关路由

网关配置了路由规则，将 `/api/user/**` 路由到用户服务，`/api/order/**` 路由到订单服务。

### 4. 熔断限流（Sentinel）

项目已集成 Sentinel，可在 Sentinel 控制台配置流控规则和降级规则。

## 常见问题

### Q: 启动报错连接不上 Nacos？

确保 Nacos 已启动，并检查配置文件中的 `spring.cloud.nacos.discovery.server-addr` 是否正确。

### Q: Feign 调用失败？

1. 确保目标服务已启动并注册到 Nacos
2. 检查服务名称是否正确
3. 查看日志确认具体错误信息

## 扩展学习

- [Spring Cloud 官方文档](https://spring.io/projects/spring-cloud)
- [Spring Cloud Alibaba 官方文档](https://github.com/alibaba/spring-cloud-alibaba/wiki)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/)

## 许可证

MIT License
