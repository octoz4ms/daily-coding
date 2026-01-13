# 前端Token过期处理完整指南

## 🎯 问题场景

当用户使用系统时，Access Token可能会在以下情况过期：
- 用户长时间未操作
- Token达到过期时间（15分钟）
- 网络问题导致请求延迟
- 多设备同时使用

前端需要优雅地处理这些情况，确保用户体验不受影响。

## 🔄 大厂级Token过期处理方案

### 核心策略：多层次防护网

```
预检查 → 智能重试 → 错误恢复 → 用户引导
```

#### 1. 请求前预检查（第一道防线）

```javascript
request: async (config) => {
    const token = this.getAccessToken();

    if (token && !isAuthRequest) {
        // 🚨 第一道防线：检查Token是否已过期
        if (this.isTokenExpired()) {
            console.warn('🚨 检测到Access Token已过期，立即刷新');

            try {
                // 同步刷新，避免发送无效Token
                await this.refreshToken();
                const newToken = this.getAccessToken();

                if (newToken) {
                    config.headers.Authorization = `Bearer ${newToken}`;
                    console.log('✅ Token刷新成功，继续请求');
                } else {
                    throw new Error('刷新失败');
                }
            } catch (error) {
                console.error('❌ 请求前Token刷新失败:', error);
                this.clearTokens();
                throw new Error('TOKEN_EXPIRED_AND_REFRESH_FAILED');
            }
        } else {
            // Token有效，正常添加
            config.headers.Authorization = `Bearer ${token}`;

            // 第二道防线：如果即将过期，异步刷新
            if (this.shouldRefresh()) {
                setTimeout(async () => {
                    await this.autoRefreshToken();
                }, 0);
            }
        }
    }

    return config;
}
```

**为什么有效？**
- ✅ 防止发送无效Token，减少401错误
- ✅ 同步处理，确保请求成功
- ✅ 性能优化，避免不必要的网络请求

#### 2. 响应错误智能重试（第二道防线）

```javascript
responseError: async (error) => {
    const { response, config } = error;

    if (response?.status === 401) {
        // 🚨 第二道防线：处理401错误

        // 防止无限重试
        if (config._retryCount >= this.maxRetries) {
            console.error('❌ 重试次数已达上限，跳转登录');
            this.clearTokens();
            this.redirectToLogin('登录已过期，请重新登录');
            return Promise.reject(new Error('TOKEN_REFRESH_EXHAUSTED'));
        }

        config._retryCount = (config._retryCount || 0) + 1;
        console.log(`🔄 401错误，尝试刷新Token (第${config._retryCount}次)`);

        try {
            await this.refreshToken();
            const newToken = this.getAccessToken();

            if (newToken) {
                config.headers.Authorization = `Bearer ${newToken}`;
                console.log('✅ 刷新成功，重试请求');
                return axios(config); // 重试原始请求
            }
        } catch (refreshError) {
            console.error('❌ Token刷新失败:', refreshError);

            if (config._retryCount >= this.maxRetries) {
                this.redirectToLogin('登录已过期，请重新登录');
            } else {
                // 指数退避重试
                const delay = 1000 * Math.pow(2, config._retryCount - 1);
                await new Promise(resolve => setTimeout(resolve, delay));
                return this.createInterceptor().responseError(error);
            }
        }
    }

    return Promise.reject(error);
}
```

**核心特性：**
- ✅ **智能重试**: 指数退避算法
- ✅ **防止死循环**: 最大重试次数限制
- ✅ **无缝体验**: 自动重试，用户无感知

#### 3. 错误恢复和用户引导（第三道防线）

```javascript
redirectToLogin(message = '请重新登录') {
    console.log('🔐 跳转登录页:', message);

    // 清理所有认证状态
    this.clearTokens();
    localStorage.removeItem('user_info');
    sessionStorage.clear();

    // 用户友好的提示
    if (typeof window.showToast === 'function') {
        window.showToast(message, 'warning');
    } else {
        alert(message);
    }

    // 智能跳转
    if (window.location.pathname !== '/login') {
        window.location.href = '/login';
    }
}
```

## 📊 处理流程详解

### 场景一：Token已过期（最常见）

```
用户请求 → 预检查发现过期 → 同步刷新 → 成功 → 继续原请求
    ↓
失败 → 抛出错误 → 跳转登录
```

### 场景二：请求时Token过期

```
发送请求 → 收到401 → 尝试刷新 → 成功 → 重试原请求
    ↓
失败 → 指数退避 → 再次尝试 → 最终跳转登录
```

### 场景三：并发请求导致过期

```
多个请求同时发送 → 部分401 → 智能重试 → 避免重复刷新
```

## 🚀 性能优化策略

### 1. 请求去重
```javascript
// 防止多个请求同时触发刷新
if (this.refreshPromise) {
    return this.refreshPromise; // 返回正在进行的刷新
}
```

### 2. 批量处理
```javascript
// 如果多个请求失败，批量刷新
this.pendingRequests = [];
// 刷新完成后统一重试
```

### 3. 缓存策略
```javascript
// 本地缓存Token状态，避免重复检查
this.tokenStatusCache = {
    status: 'valid',
    timestamp: Date.now()
};
```

## 🔧 配置参数

```javascript
const tokenManager = new TokenManager({
    baseURL: '/api',
    refreshThreshold: 300,     // 提前5分钟刷新
    maxRetries: 3,             // 最大重试3次
    retryDelay: 1000,          // 基础延迟1秒
    autoRefresh: true          // 启用自动刷新
});
```

## 📱 用户体验优化

### 1. 加载状态
```javascript
// 显示刷新中状态
this.isRefreshing = true;
// 更新UI
showLoadingIndicator();

// 刷新完成后隐藏
this.isRefreshing = false;
hideLoadingIndicator();
```

### 2. 错误提示
```javascript
// 区分不同错误类型
switch (error.code) {
    case 'TOKEN_EXPIRED':
        showToast('登录已过期，请重新登录', 'warning');
        break;
    case 'NETWORK_ERROR':
        showToast('网络异常，请检查连接', 'error');
        break;
}
```

### 3. 页面保护
```javascript
// 路由守卫
router.beforeEach((to, from, next) => {
    if (tokenManager.isTokenExpired() && to.path !== '/login') {
        next('/login');
    } else {
        next();
    }
});
```

## 🐛 常见问题及解决方案

### 1. 无限重试问题
```javascript
// 解决方案：设置最大重试次数 + 时间限制
if (config._retryCount >= this.maxRetries ||
    Date.now() - config._startTime > 30000) {
    // 停止重试
}
```

### 2. 并发刷新冲突
```javascript
// 解决方案：使用Promise去重
refreshToken() {
    if (this.refreshPromise) {
        return this.refreshPromise;
    }

    this.refreshPromise = this._doRefresh();
    return this.refreshPromise.finally(() => {
        this.refreshPromise = null;
    });
}
```

### 3. Token状态不一致
```javascript
// 解决方案：定期检查Token状态
setInterval(() => {
    this.checkTokenStatus();
}, 60000); // 每分钟检查一次
```

## 📈 监控和告警

### 关键指标
- Token刷新成功率
- 平均刷新时间
- 401错误率
- 用户登录成功率

### 日志记录
```javascript
// 重要事件记录
logger.info('Token过期处理', {
    userId,
    action: 'token_refresh',
    result: 'success',
    duration: Date.now() - startTime
});
```

## 🎯 大厂最佳实践总结

1. **多层次防护**: 预检查 + 错误处理 + 用户引导
2. **智能重试**: 指数退避 + 最大次数限制
3. **性能优化**: 请求去重 + 批量处理 + 缓存
4. **用户体验**: 无感知刷新 + 友好提示 + 平滑跳转
5. **监控告警**: 关键指标监控 + 日志记录

这样的Token过期处理方案能够确保用户在任何情况下都能获得流畅的体验，是现代Web应用的标准配置！🚀
