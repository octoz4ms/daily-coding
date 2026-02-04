package com.octo.eum.service;

import cn.hutool.core.util.IdUtil;
import com.octo.eum.security.ClientType;
import com.octo.eum.security.JwtProperties;
import com.octo.eum.security.LoginPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 会话服务 - 新方案实现
 *
 * Redis Key 设计：
 * 1. session:{sessionId}               → Hash  会话详情
 * 2. refresh:{rtid}                    → String → sessionId
 * 3. user:sessions:{userId}            → ZSet  用户所有会话（按时间排序）
 * 4. user:device:{userId}:{deviceType} → Set   同类型设备会话列表
 *
 * @author octo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redis;
    private final JwtProperties jwtProperties;

    // Redis Key 前缀
    private static final String SESSION_KEY = "session:%s";                    // session:{sid}
    private static final String REFRESH_KEY = "refresh:%s";                    // refresh:{rtid}
    private static final String USER_SESSIONS_KEY = "user:sessions:%s";        // user:sessions:{uid}
    private static final String USER_DEVICE_KEY = "user:device:%s:%s";         // user:device:{uid}:{type}

    // Session Hash 字段
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_DEVICE_TYPE = "deviceType";
    private static final String FIELD_LOGIN_TIME = "loginTime";
    private static final String FIELD_IP = "ip";
    private static final String FIELD_DEVICE_NAME = "deviceName";
    private static final String FIELD_REFRESH_TOKEN = "refreshToken";

    // ==================== 会话管理 ====================

    /**
     * 创建会话
     *
     * @return sessionId
     */
    public String createSession(Long userId, ClientType deviceType, String ip, String deviceName) {
        String sessionId = "sess_" + deviceType.getCode() + "_" + IdUtil.fastSimpleUUID().substring(0, 8);
        long loginTime = System.currentTimeMillis();

        // 1. 根据策略处理踢人
        handleKickPolicy(userId, deviceType);

        // 2. 创建 Session Hash
        String sessionKey = buildSessionKey(sessionId);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put(FIELD_USER_ID, String.valueOf(userId));
        sessionData.put(FIELD_DEVICE_TYPE, deviceType.getCode());
        sessionData.put(FIELD_LOGIN_TIME, String.valueOf(loginTime));
        sessionData.put(FIELD_IP, ip != null ? ip : "");
        sessionData.put(FIELD_DEVICE_NAME, deviceName != null ? deviceName : "");

        redis.opsForHash().putAll(sessionKey, sessionData);
        redis.expire(sessionKey, jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);

        // 3. 添加到用户会话索引（ZSET，score = loginTime）
        String userSessionsKey = buildUserSessionsKey(userId);
        redis.opsForZSet().add(userSessionsKey, sessionId, loginTime);
        redis.expire(userSessionsKey, jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);

        // 4. 添加到设备类型索引（SET）
        String userDeviceKey = buildUserDeviceKey(userId, deviceType);
        redis.opsForSet().add(userDeviceKey, sessionId);
        redis.expire(userDeviceKey, jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);

        log.info("创建会话: userId={}, deviceType={}, sessionId={}", userId, deviceType.getCode(), sessionId);
        return sessionId;
    }

    /**
     * 创建 Refresh Token
     *
     * @return rtid
     */
    public String createRefreshToken(String sessionId) {
        String rtid = "rt_" + IdUtil.fastSimpleUUID();
        String refreshKey = buildRefreshKey(rtid);

        // refresh:{rtid} → sessionId
        redis.opsForValue().set(refreshKey, sessionId,
                jwtProperties.getRefreshTokenExpiration(), TimeUnit.SECONDS);

        // 同时存到 Session Hash 中（用于刷新时验证）
        String sessionKey = buildSessionKey(sessionId);
        redis.opsForHash().put(sessionKey, FIELD_REFRESH_TOKEN, rtid);

        log.debug("创建RefreshToken: rtid={}, sessionId={}", rtid, sessionId);
        return rtid;
    }

    /**
     * 消费 Refresh Token（一次性使用）
     *
     * @return sessionId 或 null
     */
    public String consumeRefreshToken(String rtid) {
        String refreshKey = buildRefreshKey(rtid);
        String sessionId = redis.opsForValue().get(refreshKey);

        if (!StringUtils.hasText(sessionId)) {
            log.debug("RefreshToken不存在或已使用: rtid={}", rtid);
            return null;
        }

        // 验证 Session 中的 rtid 是否匹配（防止重放攻击）
        String sessionKey = buildSessionKey(sessionId);
        String storedRtid = (String) redis.opsForHash().get(sessionKey, FIELD_REFRESH_TOKEN);
        if (!rtid.equals(storedRtid)) {
            log.warn("RefreshToken不匹配，可能被盗用: rtid={}, stored={}", rtid, storedRtid);
            return null;
        }

        // 删除旧的 RefreshToken（一次性）
        redis.delete(refreshKey);
        log.debug("消费RefreshToken: rtid={}, sessionId={}", rtid, sessionId);

        return sessionId;
    }

    /**
     * 验证会话是否有效
     */
    public boolean validateSession(String sessionId) {
        String sessionKey = buildSessionKey(sessionId);
        return Boolean.TRUE.equals(redis.hasKey(sessionKey));
    }

    /**
     * 获取会话信息
     */
    public Map<Object, Object> getSessionInfo(String sessionId) {
        String sessionKey = buildSessionKey(sessionId);
        return redis.opsForHash().entries(sessionKey);
    }

    /**
     * 获取会话的用户ID
     */
    public Long getSessionUserId(String sessionId) {
        String sessionKey = buildSessionKey(sessionId);
        String userId = (String) redis.opsForHash().get(sessionKey, FIELD_USER_ID);
        return StringUtils.hasText(userId) ? Long.parseLong(userId) : null;
    }

    /**
     * 获取会话的设备类型
     */
    public ClientType getSessionDeviceType(String sessionId) {
        String sessionKey = buildSessionKey(sessionId);
        String deviceType = (String) redis.opsForHash().get(sessionKey, FIELD_DEVICE_TYPE);
        return StringUtils.hasText(deviceType) ? ClientType.fromCode(deviceType) : ClientType.UNKNOWN;
    }

    // ==================== 踢人策略 ====================

    /**
     * 根据登录策略处理踢人
     */
    private void handleKickPolicy(Long userId, ClientType deviceType) {
        LoginPolicy policy = jwtProperties.getLoginPolicy();

        switch (policy) {
            case SINGLE:
                // 单设备：踢掉所有设备
                kickAllSessions(userId);
                break;

            case SAME_TYPE_KICK:
                // 同端互踢：踢掉同类型设备
                kickSameTypeSessions(userId, deviceType);
                break;

            case MAX_DEVICE:
                // 最大设备数：超出时踢掉最早的
                handleMaxDeviceLimit(userId);
                break;

            case MULTI:
            default:
                // 多端共存：不踢人
                break;
        }
    }

    /**
     * 踢掉用户所有会话
     */
    public void kickAllSessions(Long userId) {
        String userSessionsKey = buildUserSessionsKey(userId);
        Set<String> sessionIds = redis.opsForZSet().range(userSessionsKey, 0, -1);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (String sessionId : sessionIds) {
                deleteSession(sessionId, userId);
            }
            log.info("踢掉所有会话: userId={}, count={}", userId, sessionIds.size());
        }
    }

    /**
     * 踢掉同类型设备的会话
     */
    public void kickSameTypeSessions(Long userId, ClientType deviceType) {
        String userDeviceKey = buildUserDeviceKey(userId, deviceType);
        Set<String> sessionIds = redis.opsForSet().members(userDeviceKey);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            for (String sessionId : sessionIds) {
                deleteSession(sessionId, userId);
            }
            log.info("踢掉同类型会话: userId={}, deviceType={}, count={}",
                    userId, deviceType.getCode(), sessionIds.size());
        }
    }

    /**
     * 处理最大设备数限制
     */
    private void handleMaxDeviceLimit(Long userId) {
        String userSessionsKey = buildUserSessionsKey(userId);
        Long currentCount = redis.opsForZSet().zCard(userSessionsKey);
        int maxCount = jwtProperties.getMaxDeviceCount();

        if (currentCount != null && currentCount >= maxCount) {
            // 踢掉最早登录的会话
            int kickCount = (int) (currentCount - maxCount + 1);
            Set<String> oldestSessions = redis.opsForZSet().range(userSessionsKey, 0, kickCount - 1);

            if (oldestSessions != null) {
                for (String sessionId : oldestSessions) {
                    deleteSession(sessionId, userId);
                }
                log.info("超出最大设备数，踢掉最早的: userId={}, kickCount={}", userId, kickCount);
            }
        }
    }

    /**
     * 踢掉指定会话
     */
    public boolean kickSession(String sessionId) {
        Long userId = getSessionUserId(sessionId);
        if (userId == null) {
            return false;
        }
        deleteSession(sessionId, userId);
        log.info("踢掉会话: sessionId={}", sessionId);
        return true;
    }

    /**
     * 删除会话（内部方法）
     */
    private void deleteSession(String sessionId, Long userId) {
        // 获取设备类型
        ClientType deviceType = getSessionDeviceType(sessionId);

        // 获取关联的 RefreshToken 并删除
        String sessionKey = buildSessionKey(sessionId);
        String rtid = (String) redis.opsForHash().get(sessionKey, FIELD_REFRESH_TOKEN);
        if (StringUtils.hasText(rtid)) {
            redis.delete(buildRefreshKey(rtid));
        }

        // 删除 Session Hash
        redis.delete(sessionKey);

        // 从用户会话索引中移除
        String userSessionsKey = buildUserSessionsKey(userId);
        redis.opsForZSet().remove(userSessionsKey, sessionId);

        // 从设备类型索引中移除
        if (deviceType != ClientType.UNKNOWN) {
            String userDeviceKey = buildUserDeviceKey(userId, deviceType);
            redis.opsForSet().remove(userDeviceKey, sessionId);
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取用户所有会话（按时间排序）
     */
    public List<Map<String, Object>> getUserSessions(Long userId) {
        String userSessionsKey = buildUserSessionsKey(userId);
        Set<String> sessionIds = redis.opsForZSet().reverseRange(userSessionsKey, 0, -1);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sessionIds.stream()
                .map(sessionId -> {
                    Map<Object, Object> sessionData = getSessionInfo(sessionId);
                    if (sessionData.isEmpty()) {
                        return null;
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("sessionId", sessionId);
                    result.put("userId", sessionData.get(FIELD_USER_ID));
                    result.put("deviceType", sessionData.get(FIELD_DEVICE_TYPE));
                    result.put("loginTime", sessionData.get(FIELD_LOGIN_TIME));
                    result.put("ip", sessionData.get(FIELD_IP));
                    result.put("deviceName", sessionData.get(FIELD_DEVICE_NAME));
                    return result;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户在线设备数
     */
    public long getUserSessionCount(Long userId) {
        String userSessionsKey = buildUserSessionsKey(userId);
        Long count = redis.opsForZSet().zCard(userSessionsKey);
        return count != null ? count : 0;
    }

    /**
     * 获取用户同类型设备数
     */
    public long getUserDeviceTypeCount(Long userId, ClientType deviceType) {
        String userDeviceKey = buildUserDeviceKey(userId, deviceType);
        Long count = redis.opsForSet().size(userDeviceKey);
        return count != null ? count : 0;
    }

    // ==================== Key 构建方法 ====================

    private String buildSessionKey(String sessionId) {
        return String.format(SESSION_KEY, sessionId);
    }

    private String buildRefreshKey(String rtid) {
        return String.format(REFRESH_KEY, rtid);
    }

    private String buildUserSessionsKey(Long userId) {
        return String.format(USER_SESSIONS_KEY, userId);
    }

    private String buildUserDeviceKey(Long userId, ClientType deviceType) {
        return String.format(USER_DEVICE_KEY, userId, deviceType.getCode());
    }
}

