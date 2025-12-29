# Enterprise User Management System

企业级用户管理系统 - 基于 Spring Boot 3.x + Spring Security 6.x 的标准化实现

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.1 | 基础框架 |
| Spring Security | 6.x | 安全框架 |
| MyBatis Plus | 3.5.5 | ORM框架 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存、Token存储 |
| RabbitMQ | 3.x | 消息队列 |
| JWT | 0.12.3 | Token认证 |
| Hutool | 5.8.24 | 工具库 |

## 项目结构

```
enterprise-user-management/
├── src/main/java/com/octo/eum/
│   ├── common/           # 通用类(Result, ResultCode, PageResult)
│   ├── config/           # 配置类(Redis, RabbitMQ, MyBatisPlus, WebMvc)
│   ├── controller/       # 控制器层
│   ├── dto/              # 数据传输对象
│   │   ├── request/      # 请求DTO
│   │   └── response/     # 响应DTO/VO
│   ├── entity/           # 实体类
│   ├── exception/        # 异常处理
│   ├── mapper/           # MyBatis Mapper接口
│   ├── mq/               # 消息队列消费者
│   ├── security/         # Spring Security相关
│   ├── service/          # 服务层
│   │   └── impl/         # 服务实现
│   └── util/             # 工具类
├── src/main/resources/
│   ├── mapper/           # MyBatis XML映射文件
│   ├── db/               # 数据库脚本
│   └── application.yml   # 配置文件
└── pom.xml
```

## 功能特性

### 认证授权
- [x] JWT Token认证
- [x] Token刷新机制
- [x] Token黑名单(Redis)
- [x] 基于角色的访问控制(RBAC)
- [x] 方法级权限控制(@PreAuthorize)

### 用户管理
- [x] 用户CRUD
- [x] 用户状态管理
- [x] 密码加密(BCrypt)
- [x] 密码修改/重置
- [x] 角色分配

### 角色管理
- [x] 角色CRUD
- [x] 权限分配
- [x] 角色状态管理

### 权限管理
- [x] 权限CRUD
- [x] 树形权限结构
- [x] 菜单/按钮权限

### 日志管理
- [x] 登录日志记录
- [x] 异步日志(RabbitMQ)

### 其他特性
- [x] 统一响应格式
- [x] 全局异常处理
- [x] 参数校验
- [x] 分页查询
- [x] 跨域支持
- [x] 缓存支持(Redis)

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Redis 7.x
- RabbitMQ 3.x

### 初始化数据库

```sql
-- 执行数据库初始化脚本
source src/main/resources/db/schema.sql
```

### 修改配置

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/eum_db
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
```

### 启动项目

```bash
mvn spring-boot:run
```

## API接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/logout | 用户登出 |
| POST | /api/auth/refresh | 刷新Token |
| GET | /api/auth/info | 获取当前用户信息 |

### 用户管理接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/users | 分页查询用户 | user:view |
| GET | /api/users/{id} | 获取用户详情 | user:view |
| POST | /api/users | 创建用户 | user:create |
| PUT | /api/users | 更新用户 | user:update |
| DELETE | /api/users/{id} | 删除用户 | user:delete |
| PUT | /api/users/password | 修改密码 | 登录用户 |
| PUT | /api/users/{id}/reset-password | 重置密码 | user:reset-password |
| PUT | /api/users/{id}/status | 更新状态 | user:update |
| PUT | /api/users/{id}/roles | 分配角色 | user:assign-role |

### 角色管理接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/roles | 分页查询角色 | role:view |
| GET | /api/roles/list | 获取所有角色 | role:view |
| GET | /api/roles/{id} | 获取角色详情 | role:view |
| POST | /api/roles | 创建角色 | role:create |
| PUT | /api/roles | 更新角色 | role:update |
| DELETE | /api/roles/{id} | 删除角色 | role:delete |
| PUT | /api/roles/{id}/permissions | 分配权限 | role:assign-permission |

### 权限管理接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | /api/permissions/tree | 获取权限树 | permission:view |
| GET | /api/permissions/list | 获取所有权限 | permission:view |
| GET | /api/permissions/menu | 获取当前用户菜单 | 登录用户 |
| GET | /api/permissions/{id} | 获取权限详情 | permission:view |
| POST | /api/permissions | 创建权限 | permission:create |
| PUT | /api/permissions | 更新权限 | permission:update |
| DELETE | /api/permissions/{id} | 删除权限 | permission:delete |

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | ADMIN | 超级管理员，拥有所有权限 |
| user | admin123 | USER | 普通用户，只有查看权限 |

## 请求示例

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### 带Token请求

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer <your_token>"
```

## License

MIT License

