package com.octo.shorturl.service;

import com.octo.shorturl.entity.AccessLog;
import com.octo.shorturl.mapper.AccessLogMapper;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问统计服务
 * 
 * 记录访问日志，提供统计分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessStatService {

    private final AccessLogMapper accessLogMapper;

    /**
     * 异步记录访问日志
     */
    @Async
    public void recordAccess(String shortCode, HttpServletRequest request) {
        try {
            AccessLog accessLog = new AccessLog();
            accessLog.setShortCode(shortCode);
            accessLog.setIp(getClientIp(request));
            accessLog.setUserAgent(request.getHeader("User-Agent"));
            accessLog.setReferer(request.getHeader("Referer"));
            accessLog.setAccessTime(LocalDateTime.now());

            // 解析User-Agent
            parseUserAgent(accessLog);

            accessLogMapper.insert(accessLog);
            
            log.debug("记录访问日志: shortCode={}, ip={}", shortCode, accessLog.getIp());
        } catch (Exception e) {
            log.error("记录访问日志失败", e);
        }
    }

    /**
     * 获取短链接统计数据
     */
    public Map<String, Object> getStats(String shortCode) {
        Map<String, Object> stats = new HashMap<>();
        
        // PV和UV
        stats.put("pv", accessLogMapper.countPv(shortCode));
        stats.put("uv", accessLogMapper.countUv(shortCode));
        
        // 最近7天的访问趋势
        LocalDate today = LocalDate.now();
        stats.put("dailyPv", accessLogMapper.countPvByDate(shortCode, 
                today.minusDays(7), today.plusDays(1)));
        
        // 设备类型分布
        stats.put("deviceTypes", accessLogMapper.countByDeviceType(shortCode));
        
        // 浏览器分布
        stats.put("browsers", accessLogMapper.countByBrowser(shortCode));
        
        return stats;
    }

    /**
     * 获取短链接PV
     */
    public long getPv(String shortCode) {
        return accessLogMapper.countPv(shortCode);
    }

    /**
     * 获取短链接UV
     */
    public long getUv(String shortCode) {
        return accessLogMapper.countUv(shortCode);
    }

    /**
     * 解析User-Agent
     */
    private void parseUserAgent(AccessLog accessLog) {
        String userAgentStr = accessLog.getUserAgent();
        if (userAgentStr == null || userAgentStr.isEmpty()) {
            return;
        }

        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentStr);
        
        Browser browser = userAgent.getBrowser();
        if (browser != null) {
            accessLog.setBrowser(browser.getName());
        }
        
        OperatingSystem os = userAgent.getOperatingSystem();
        if (os != null) {
            accessLog.setOs(os.getName());
            DeviceType deviceType = os.getDeviceType();
            if (deviceType != null) {
                accessLog.setDeviceType(deviceType.getName());
            }
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

