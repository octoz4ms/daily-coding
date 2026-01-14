package com.octo.eum.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 设备识别工具类
 *
 * @author octo
 */
public class DeviceUtils {

    private DeviceUtils() {
    }

    /**
     * 从请求中识别设备类型
     * 优先使用请求头 X-Device-Type，否则通过 User-Agent 解析
     */
    public static DeviceType getDeviceType(HttpServletRequest request) {
        // 优先使用前端传递的设备类型
        String deviceTypeHeader = request.getHeader("X-Device-Type");
        if (StringUtils.hasText(deviceTypeHeader)) {
            return DeviceType.fromCode(deviceTypeHeader);
        }

        // 通过 User-Agent 解析
        String userAgent = request.getHeader("User-Agent");
        return parseDeviceType(userAgent);
    }

    /**
     * 解析 User-Agent 获取设备类型
     */
    private static DeviceType parseDeviceType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return DeviceType.UNKNOWN;
        }

        String ua = userAgent.toLowerCase();

        // 判断是否为平板（先判断平板，因为有些平板 UA 也包含 mobile）
        if (ua.contains("ipad") || 
            (ua.contains("android") && !ua.contains("mobile"))) {
            return DeviceType.TABLET;
        }

        // 判断是否为手机
        if (ua.contains("mobile") || 
            ua.contains("iphone") || 
            ua.contains("android") ||
            ua.contains("phone")) {
            return DeviceType.MOBILE;
        }

        // 判断是否为电脑
        if (ua.contains("windows") || 
            ua.contains("macintosh") || 
            ua.contains("mac os") ||
            ua.contains("linux") && !ua.contains("android")) {
            return DeviceType.PC;
        }

        return DeviceType.UNKNOWN;
    }
}

