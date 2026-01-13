/**
 * 前端Token管理工具类 - 大厂级Token管理策略
 * 提供自动刷新、状态检查、本地存储等功能
 */
class TokenManager {
    constructor(options = {}) {
        this.baseURL = options.baseURL || '/api';
        this.storageKey = options.storageKey || 'auth_tokens';
        this.refreshThreshold = options.refreshThreshold || 300; // 5分钟
        this.autoRefresh = options.autoRefresh !== false;
        this.maxRetries = options.maxRetries || 3;

        this.tokens = this.loadTokens();
        this.refreshPromise = null;

        if (this.autoRefresh) {
            this.startAutoRefresh();
        }
    }

    /**
     * 存储Token
     */
    storeTokens(accessToken, refreshToken, expiresIn) {
        const tokens = {
            accessToken,
            refreshToken,
            expiresAt: Date.now() + (expiresIn * 1000),
            storedAt: Date.now()
        };

        this.tokens = tokens;
        localStorage.setItem(this.storageKey, JSON.stringify(tokens));

        console.log('Token已存储');
    }

    /**
     * 获取Access Token
     */
    getAccessToken() {
        if (!this.tokens || this.isExpired()) {
            return null;
        }
        return this.tokens.accessToken;
    }

    /**
     * 获取Refresh Token
     */
    getRefreshToken() {
        return this.tokens?.refreshToken || null;
    }

    /**
     * 检查Token是否过期
     */
    isExpired() {
        if (!this.tokens) return true;
        return Date.now() >= this.tokens.expiresAt;
    }

    /**
     * 检查是否需要刷新
     */
    shouldRefresh() {
        if (!this.tokens) return false;
        const remainingTime = this.tokens.expiresAt - Date.now();
        return remainingTime <= (this.refreshThreshold * 1000);
    }

    /**
     * 获取剩余时间（秒）
     */
    getRemainingTime() {
        if (!this.tokens) return 0;
        return Math.max(0, Math.floor((this.tokens.expiresAt - Date.now()) / 1000));
    }

    /**
     * 刷新Token
     */
    async refreshToken() {
        if (this.refreshPromise) {
            return this.refreshPromise;
        }

        this.refreshPromise = this._doRefresh();

        try {
            const result = await this.refreshPromise;
            return result;
        } finally {
            this.refreshPromise = null;
        }
    }

    async _doRefresh() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            throw new Error('没有可用的Refresh Token');
        }

        for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
            try {
                console.log(`尝试刷新Token (第${attempt}次)`);

                const response = await fetch(`${this.baseURL}/auth/refresh`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Refresh-Token': refreshToken
                    }
                });

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                const data = await response.json();

                if (data.code === 200) {
                    this.storeTokens(
                        data.data.accessToken,
                        data.data.refreshToken,
                        data.data.expiresIn
                    );

                    console.log('Token刷新成功');
                    return data.data;
                } else {
                    throw new Error(data.message || '刷新失败');
                }

            } catch (error) {
                console.error(`Token刷新失败 (第${attempt}次):`, error);

                if (attempt === this.maxRetries) {
                    // 刷新失败，清除Token
                    this.clearTokens();
                    throw error;
                }

                // 等待后重试
                await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
            }
        }
    }

    /**
     * 自动刷新Access Token
     */
    async autoRefreshToken() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            throw new Error('没有可用的Access Token');
        }

        try {
            console.log('自动刷新Access Token');

            const response = await fetch(`${this.baseURL}/auth/auto-refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (data.code === 200) {
                this.storeTokens(
                    data.data.accessToken,
                    data.data.refreshToken || this.getRefreshToken(),
                    data.data.expiresIn
                );

                console.log('Access Token自动刷新成功');
                return data.data;
            } else {
                throw new Error(data.message || '自动刷新失败');
            }

        } catch (error) {
            console.error('Access Token自动刷新失败:', error);
            // 如果自动刷新失败，尝试使用Refresh Token
            return this.refreshToken();
        }
    }

    /**
     * 检查Token状态
     */
    async checkTokenStatus() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            return { valid: false };
        }

        try {
            const response = await fetch(`${this.baseURL}/auth/token-status`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (!response.ok) {
                return { valid: false };
            }

            const data = await response.json();
            return data.code === 200 ? data.data : { valid: false };

        } catch (error) {
            console.error('检查Token状态失败:', error);
            return { valid: false };
        }
    }

    /**
     * 获取认证头
     */
    getAuthHeader() {
        const token = this.getAccessToken();
        return token ? `Bearer ${token}` : null;
    }

    /**
     * 启动自动刷新
     */
    startAutoRefresh() {
        if (!this.autoRefresh) return;

        const checkAndRefresh = async () => {
            try {
                if (this.shouldRefresh()) {
                    console.log('检测到Token需要刷新');
                    await this.autoRefreshToken();
                }
            } catch (error) {
                console.error('自动刷新失败:', error);
            }
        };

        // 每分钟检查一次
        setInterval(checkAndRefresh, 60000);

        // 页面可见性变化时检查
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && this.shouldRefresh()) {
                checkAndRefresh();
            }
        });
    }

    /**
     * 清除Token
     */
    clearTokens() {
        this.tokens = null;
        localStorage.removeItem(this.storageKey);
        console.log('Token已清除');
    }

    /**
     * 加载Token
     */
    loadTokens() {
        try {
            const stored = localStorage.getItem(this.storageKey);
            return stored ? JSON.parse(stored) : null;
        } catch (error) {
            console.error('加载Token失败:', error);
            return null;
        }
    }

    /**
     * 获取Token状态
     */
    getTokenStatus() {
        return {
            hasToken: !!this.tokens,
            isExpired: this.isExpired(),
            shouldRefresh: this.shouldRefresh(),
            remainingTime: this.getRemainingTime(),
            expiresAt: this.tokens?.expiresAt || null
        };
    }

    /**
     * 拦截器 - 用于Axios等HTTP库
     */
    createInterceptor() {
        return {
            request: async (config) => {
                // 检查是否为认证相关请求
                const isAuthRequest = config.url.includes('/auth/') ||
                                    config.url.includes('/login');

                // 获取当前Token
                const token = this.getAccessToken();

                // 如果有Token且不是认证请求，添加Authorization头
                if (token && !isAuthRequest) {
                    // 预检查：Token是否已经过期
                    if (this.isTokenExpired()) {
                        console.warn('检测到Access Token已过期，尝试刷新');

                        try {
                            // 同步刷新Token，避免发送过期Token
                            await this.refreshToken();
                            const newToken = this.getAccessToken();
                            if (newToken) {
                                config.headers.Authorization = `Bearer ${newToken}`;
                                console.log('Token刷新成功，继续请求');
                            } else {
                                throw new Error('刷新后仍无有效Token');
                            }
                        } catch (refreshError) {
                            console.error('请求前Token刷新失败:', refreshError);
                            this.clearTokens();
                            // 抛出特定错误，前端可以处理跳转登录
                            throw new Error('TOKEN_EXPIRED_AND_REFRESH_FAILED');
                        }
                    } else {
                        // Token未过期，正常添加
                        config.headers.Authorization = `Bearer ${token}`;

                        // 如果Token即将过期，提前刷新（异步，不阻塞请求）
                        if (this.shouldRefresh()) {
                            console.log('Token即将过期，异步刷新');
                            // 异步刷新，不阻塞当前请求
                            setTimeout(async () => {
                                try {
                                    await this.autoRefreshToken();
                                } catch (error) {
                                    console.warn('异步Token刷新失败:', error);
                                }
                            }, 0);
                        }
                    }
                }

                return config;
            },

            response: async (response) => {
                // 检查响应头是否有Token刷新提示
                const refreshHeader = response.headers['x-token-refresh'];
                if (refreshHeader === 'refresh-needed') {
                    console.log('服务端提示Token需要刷新');

                    // 异步刷新Token，不阻塞当前响应
                    setTimeout(async () => {
                        try {
                            await this.autoRefreshToken();
                            console.log('根据服务端提示成功刷新Token');
                        } catch (error) {
                            console.warn('根据服务端提示刷新Token失败:', error);
                        }
                    }, 100); // 短暂延迟，避免与当前请求冲突
                }

                return response;
            },

            responseError: async (error) => {
                const { response, config } = error;

                // 401错误处理 - Token过期或无效
                if (response?.status === 401) {
                    // 避免无限重试
                    if (config._retryCount >= this.maxRetries) {
                        console.error('Token刷新重试次数已达上限');
                        this.clearTokens();
                        this.redirectToLogin('Token刷新失败，请重新登录');
                        return Promise.reject(new Error('TOKEN_REFRESH_EXHAUSTED'));
                    }

                    // 标记重试次数
                    config._retryCount = (config._retryCount || 0) + 1;

                    console.log(`检测到401错误，尝试刷新Token (第${config._retryCount}次)`);

                    try {
                        // 尝试刷新Token
                        await this.refreshToken();
                        const newToken = this.getAccessToken();

                        if (newToken) {
                            // 更新请求头
                            config.headers.Authorization = `Bearer ${newToken}`;
                            console.log('Token刷新成功，重试原始请求');
                            // 重试原始请求
                            return axios(config);
                        } else {
                            throw new Error('刷新后无有效Token');
                        }
                    } catch (refreshError) {
                        console.error('Token刷新失败:', refreshError);

                        // 如果是最后一次重试，清除Token并跳转登录
                        if (config._retryCount >= this.maxRetries) {
                            this.clearTokens();
                            this.redirectToLogin('登录已过期，请重新登录');
                            return Promise.reject(new Error('TOKEN_EXPIRED'));
                        }

                        // 等待后重试（指数退避）
                        const delay = 1000 * Math.pow(2, config._retryCount - 1);
                        await new Promise(resolve => setTimeout(resolve, delay));

                        // 递归重试
                        return this.createInterceptor().responseError(error);
                    }
                }

                // 403错误 - 权限不足
                else if (response?.status === 403) {
                    console.error('权限不足:', response.data?.message);
                    // 可以显示权限不足提示或跳转到无权限页面
                    if (typeof window.showToast === 'function') {
                        window.showToast('权限不足', 'error');
                    }
                    return Promise.reject(new Error('PERMISSION_DENIED'));
                }

                // 其他错误直接抛出
                return Promise.reject(error);
            }
        };
    }

    /**
     * 跳转到登录页
     */
    redirectToLogin(message = '请重新登录') {
        console.log('跳转到登录页:', message);

        // 清除所有认证相关数据
        this.clearTokens();
        localStorage.removeItem('user_info');
        sessionStorage.clear();

        // 显示提示消息
        if (typeof window.showToast === 'function') {
            window.showToast(message, 'warning');
        } else if (typeof alert === 'function') {
            alert(message);
        }

        // 跳转到登录页（避免重复跳转）
        if (window.location.pathname !== '/login') {
            window.location.href = '/login';
        }
    }
}

// 使用示例
const tokenManager = new TokenManager({
    baseURL: '/api',
    refreshThreshold: 300, // 5分钟
    autoRefresh: true
});

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = TokenManager;
} else if (typeof define === 'function' && define.amd) {
    define([], () => TokenManager);
} else {
    window.TokenManager = TokenManager;
}
