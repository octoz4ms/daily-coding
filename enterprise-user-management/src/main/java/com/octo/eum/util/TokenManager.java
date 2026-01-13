package com.octo.eum.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Token管理工具类 - 前端集成工具
 * 提供自动刷新、状态检查等功能
 *
 * @author octo
 */
@Slf4j
@Component
public class TokenManager {

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
    private final ConcurrentHashMap<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * Token信息
     */
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
        private long expiresAt;
        private long refreshThreshold; // 自动刷新阈值（秒）
        private boolean autoRefresh;
        private Runnable refreshCallback;

        public TokenInfo(String accessToken, String refreshToken, long expiresIn, long refreshThreshold) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = System.currentTimeMillis() + (expiresIn * 1000);
            this.refreshThreshold = refreshThreshold;
            this.autoRefresh = true;
        }

        public boolean shouldRefresh() {
            long remainingTime = expiresAt - System.currentTimeMillis();
            return remainingTime <= (refreshThreshold * 1000);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }

        // Getters and setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Runnable getRefreshCallback() { return refreshCallback; }
        public void setRefreshCallback(Runnable refreshCallback) { this.refreshCallback = refreshCallback; }
        public boolean isAutoRefresh() { return autoRefresh; }
        public void setAutoRefresh(boolean autoRefresh) { this.autoRefresh = autoRefresh; }
    }

    /**
     * 存储Token信息
     */
    public void storeToken(String key, String accessToken, String refreshToken, long expiresIn, long refreshThreshold) {
        TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken, expiresIn, refreshThreshold);
        tokenCache.put(key, tokenInfo);

        // 启动自动刷新任务
        startAutoRefreshTask(key, tokenInfo);
    }

    /**
     * 获取Token
     */
    public String getAccessToken(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        return tokenInfo != null ? tokenInfo.getAccessToken() : null;
    }

    /**
     * 获取RefreshToken
     */
    public String getRefreshToken(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        return tokenInfo != null ? tokenInfo.getRefreshToken() : null;
    }

    /**
     * 更新Token
     */
    public void updateToken(String key, String newAccessToken, long expiresIn) {
        TokenInfo tokenInfo = tokenCache.get(key);
        if (tokenInfo != null) {
            tokenInfo.setAccessToken(newAccessToken);
            tokenInfo.expiresAt = System.currentTimeMillis() + (expiresIn * 1000);

            // 重新启动自动刷新任务
            startAutoRefreshTask(key, tokenInfo);
        }
    }

    /**
     * 移除Token
     */
    public void removeToken(String key) {
        tokenCache.remove(key);
    }

    /**
     * 检查Token是否需要刷新
     */
    public boolean shouldRefreshToken(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        return tokenInfo != null && tokenInfo.shouldRefresh();
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        return tokenInfo == null || tokenInfo.isExpired();
    }

    /**
     * 获取Token剩余时间（秒）
     */
    public long getTokenRemainingTime(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        if (tokenInfo == null) return 0;
        return Math.max(0, (tokenInfo.expiresAt - System.currentTimeMillis()) / 1000);
    }

    /**
     * 设置自动刷新回调
     */
    public void setRefreshCallback(String key, Runnable callback) {
        TokenInfo tokenInfo = tokenCache.get(key);
        if (tokenInfo != null) {
            tokenInfo.setRefreshCallback(callback);
        }
    }

    /**
     * 手动触发Token刷新
     */
    public void triggerRefresh(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        if (tokenInfo != null && tokenInfo.getRefreshCallback() != null) {
            try {
                tokenInfo.getRefreshCallback().run();
                log.debug("手动触发Token刷新成功: {}", key);
            } catch (Exception e) {
                log.error("手动触发Token刷新失败: {}", key, e);
            }
        }
    }

    /**
     * 启动自动刷新任务
     */
    private void startAutoRefreshTask(String key, TokenInfo tokenInfo) {
        if (!tokenInfo.isAutoRefresh() || tokenInfo.getRefreshCallback() == null) {
            return;
        }

        long delay = Math.max(1, tokenInfo.refreshThreshold / 2); // 提前一半时间检查

        scheduler.schedule(() -> {
            try {
                if (tokenCache.containsKey(key) && tokenInfo.shouldRefresh()) {
                    log.debug("自动刷新Token: {}", key);
                    tokenInfo.getRefreshCallback().run();
                }
            } catch (Exception e) {
                log.error("自动刷新Token失败: {}", key, e);
                // 失败后重试
                scheduler.schedule(() -> startAutoRefreshTask(key, tokenInfo), 30, TimeUnit.SECONDS);
            }
        }, delay, TimeUnit.SECONDS);
    }

    /**
     * 获取Token状态信息
     */
    public TokenStatus getTokenStatus(String key) {
        TokenInfo tokenInfo = tokenCache.get(key);
        if (tokenInfo == null) {
            return new TokenStatus(false, 0, false, false);
        }

        return new TokenStatus(
            !tokenInfo.isExpired(),
            getTokenRemainingTime(key),
            tokenInfo.shouldRefresh(),
            tokenInfo.isAutoRefresh()
        );
    }

    /**
     * Token状态
     */
    public static class TokenStatus {
        private final boolean valid;
        private final long remainingTime;
        private final boolean shouldRefresh;
        private final boolean autoRefresh;

        public TokenStatus(boolean valid, long remainingTime, boolean shouldRefresh, boolean autoRefresh) {
            this.valid = valid;
            this.remainingTime = remainingTime;
            this.shouldRefresh = shouldRefresh;
            this.autoRefresh = autoRefresh;
        }

        public boolean isValid() { return valid; }
        public long getRemainingTime() { return remainingTime; }
        public boolean isShouldRefresh() { return shouldRefresh; }
        public boolean isAutoRefresh() { return autoRefresh; }
    }

    /**
     * 清理过期Token
     */
    public void cleanupExpiredTokens() {
        tokenCache.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                log.debug("清理过期Token: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
