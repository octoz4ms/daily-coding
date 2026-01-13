# 大厂级Token管理系统

本系统实现了企业级Token管理的最佳实践，包含自动刷新、指纹校验、版本控制、安全增强等大厂级特性。

## 🚀 核心特性

### 1. 双Token策略
- **Access Token**: 15分钟过期（可配置）
- **Refresh Token**: 30天过期（可配置）
- 支持滑动窗口续签机制

### 2. 自动刷新机制
- 前端自动检测Token即将过期
- 支持并发请求保护
- 滑动窗口续签，避免频繁刷新

### 3. 安全增强
- **指纹校验**: 基于IP + User-Agent
- **版本控制**: 支持强制失效旧Token
- **单设备登录**: 可配置单设备登录
- **重放攻击防护**: 防止Token重放
- **频率限制**: 防止恶意请求
- **Token泄露检测**: 多IP使用告警

### 4. 前端集成
- 完整的JavaScript工具类
- 支持Axios等HTTP库拦截器
- 本地存储和状态管理
- 自动错误处理和重试

## 📋 配置说明

### 后端配置 (application.yml)

```yaml
jwt:
  secret: your-256-bit-secret-key-must-be-at-least-32-characters-long-for-production
  access-token-expiration: 900  # 15分钟
  refresh-token-expiration: 2592000  # 30天
  auto-refresh-threshold: 300  # 5分钟前自动刷新
  single-device-login: false  # 单设备登录
  enable-fingerprint: true  # 指纹校验
  enable-sliding-refresh: true  # 滑动窗口续签
  max-refresh-count: 10  # 最大续签次数
  token-version: 1  # Token版本控制
  enable-concurrent-refresh-protection: true  # 并发刷新保护
  refresh-lock-timeout: 30  # 刷新锁超时
```

## 🔧 API接口

### 认证接口

```http
# 用户登录
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}

# 刷新Token
POST /api/auth/refresh
X-Refresh-Token: <refresh_token>

# 自动刷新Access Token
POST /api/auth/auto-refresh
Authorization: Bearer <access_token>

# 检查Token状态
GET /api/auth/token-status
Authorization: Bearer <access_token>

# 用户登出
POST /api/auth/logout
Authorization: Bearer <access_token>
```

### 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员"
    },
    "roles": ["ADMIN"],
    "permissions": ["user:read", "user:write"]
  }
}
```

## 💻 前端使用

### 1. 初始化Token管理器

```javascript
const tokenManager = new TokenManager({
    baseURL: '/api',
    refreshThreshold: 300, // 5分钟
    autoRefresh: true,
    maxRetries: 3
});
```

### 2. 登录处理

```javascript
async function login(username, password) {
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();
        if (data.code === 200) {
            tokenManager.storeTokens(
                data.data.accessToken,
                data.data.refreshToken,
                data.data.expiresIn
            );
            return data.data;
        }
    } catch (error) {
        console.error('登录失败:', error);
    }
}
```

### 3. Axios拦截器集成

```javascript
// 创建拦截器
const interceptor = tokenManager.createInterceptor();

// 配置Axios
axios.interceptors.request.use(interceptor.request);
axios.interceptors.response.use(
    response => response,
    interceptor.response
);

// 现在可以正常使用Axios
const response = await axios.get('/api/users');
```

### 4. Token刷新机制详解

#### 响应头提示方案 ⭐推荐
服务端检测到Token即将过期时，在响应头中添加提示：
```
X-Token-Refresh: refresh-needed
X-Token-Refresh-Message: Token即将过期，建议刷新
X-Token-Refresh-Url: /api/auth/auto-refresh
```

前端自动监听并异步刷新：
```javascript
response: async (response) => {
    const refreshHeader = response.headers['x-token-refresh'];
    if (refreshHeader === 'refresh-needed') {
        console.log('服务端提示Token需要刷新');
        // 异步刷新，不阻塞当前响应
        setTimeout(async () => {
            await this.autoRefreshToken();
        }, 100);
    }
    return response;
}
```

#### 自动刷新配置
```javascript
const tokenManager = new TokenManager({
    refreshThreshold: 300,    // 5分钟前开始刷新
    autoRefresh: true,        // 启用自动刷新
    maxRetries: 3            // 最大重试次数
});
```

### 5. Token过期处理

#### 智能过期检测
```javascript
// 检查Token是否过期
const isExpired = tokenManager.isTokenExpired();

// 检查是否需要刷新（提前5分钟）
const shouldRefresh = tokenManager.shouldRefresh();

// 获取剩余时间
const remainingTime = tokenManager.getTokenRemainingTime();
```

#### 自动过期处理
系统自动处理Token过期，无需手动干预：

1. **请求前预检查**: 发现过期立即刷新
2. **401错误自动重试**: 失败后自动刷新并重试
3. **智能跳转**: 多次失败后跳转登录页

```javascript
// 拦截器自动处理，无需额外代码
axios.interceptors.request.use(tokenManager.createInterceptor().request);
axios.interceptors.response.use(
    response => response,
    tokenManager.createInterceptor().responseError
);
```

### 6. 手动Token操作

```javascript
// 获取当前Token
const token = tokenManager.getAccessToken();

// 检查Token状态
const status = tokenManager.getTokenStatus();
console.log('Token状态:', status);

// 手动刷新
await tokenManager.refreshToken();

// 清除Token并跳转登录
tokenManager.clearTokens();
tokenManager.redirectToLogin('会话已过期');
```

## 🔒 安全特性详解

### 指纹校验
系统基于IP地址和User-Agent生成唯一指纹，防止Token被盗用：

```javascript
// 指纹生成逻辑
fingerprint = hash(ip + "|" + userAgent)
```

### 版本控制
支持Token版本控制，管理员可以强制让所有旧Token失效：

```javascript
// 修改配置中的token-version即可
jwt.token-version: 2
```

### 单设备登录
启用后，新登录会使其他设备Token失效：

```yaml
jwt.single-device-login: true
```

### 并发保护
防止多个请求同时刷新Token：

```javascript
// 使用Redis分布式锁
SET refresh:lock:{userId} EX 30 NX
```

## 📊 监控和告警

### Token状态监控
```javascript
// 检查Token状态
const status = await tokenManager.checkTokenStatus();
console.log('Token有效性:', status.valid);
console.log('剩余时间:', status.remainingTime);
console.log('需要刷新:', status.shouldRefresh);
```

### 安全事件日志
系统会记录以下安全事件：
- 重放攻击检测
- 频率限制触发
- Token泄露风险
- 可疑登录活动

## 🛠️ 最佳实践

### 1. 前端配置
```javascript
const tokenManager = new TokenManager({
    baseURL: process.env.API_BASE_URL,
    refreshThreshold: 300,
    autoRefresh: true,
    maxRetries: 3
});
```

### 2. 错误处理
```javascript
try {
    const response = await axios.get('/api/secure-data');
} catch (error) {
    if (error.response?.status === 401) {
        // Token失效，跳转登录页
        window.location.href = '/login';
    }
}
```

### 3. Token清理
```javascript
// 页面卸载时清理
window.addEventListener('beforeunload', () => {
    tokenManager.clearTokens();
});
```

## 🚀 性能优化

### Redis存储优化
- 使用Hash存储用户Token信息
- 设置合理的过期时间
- 启用Redis连接池

### 前端缓存策略
- localStorage持久化存储
- 内存缓存加速访问
- 懒加载Token状态检查

## 🔧 运维指南

### 配置监控
```bash
# 检查Redis中Token数量
redis-cli KEYS "auth:token:*" | wc -l

# 检查黑名单Token
redis-cli KEYS "auth:blacklist:*" | wc -l
```

### 日志分析
```bash
# 查看安全事件
grep "指纹校验失败\|重放攻击\|频率限制" logs/app.log

# Token刷新统计
grep "Token刷新成功" logs/app.log | wc -l
```

### 故障排查
1. **Token频繁失效**: 检查指纹校验配置
2. **刷新失败**: 检查Redis连接和网络
3. **性能问题**: 监控Redis内存使用

## 📈 扩展功能

### 多设备管理
```java
// 获取用户设备列表
List<String> devices = authService.getUserDevices(userId);

// 强制下线设备
authService.forceLogoutDevice(userId, deviceId);
```

### Token审计
```java
// 记录Token使用日志
tokenAuditService.logTokenUsage(token, request);
```

### 自定义校验器
```java
@Component
public class CustomTokenValidator implements TokenValidator {
    @Override
    public boolean validate(TokenContext context) {
        // 自定义验证逻辑
        return true;
    }
}
```

## 🤝 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交变更
4. 发起Pull Request

## 📄 许可证

本项目采用MIT许可证。
