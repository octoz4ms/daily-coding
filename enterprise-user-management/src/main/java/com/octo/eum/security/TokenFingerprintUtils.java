package com.octo.eum.security;

import com.octo.eum.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Token指纹工具类 - 基于IP和User-Agent生成唯一指纹
 * 用于增强Token安全性，防止Token被盗用
 *
 * @author octo
 */
@Slf4j
@Component
public class TokenFingerprintUtils {

    private static final String FINGERPRINT_SEPARATOR = "|";

    /**
     * 生成Token指纹
     *
     * @param request HTTP请求
     * @return 指纹字符串
     */
    public String generateFingerprint(HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (!StringUtils.hasText(ip)) {
            ip = "unknown";
        }
        if (!StringUtils.hasText(userAgent)) {
            userAgent = "unknown";
        }

        String fingerprint = ip + FINGERPRINT_SEPARATOR + userAgent;
        return hashFingerprint(fingerprint);
    }

    /**
     * 验证Token指纹
     *
     * @param request    HTTP请求
     * @param storedFingerprint 存储的指纹
     * @return 是否匹配
     */
    public boolean validateFingerprint(HttpServletRequest request, String storedFingerprint) {
        if (!StringUtils.hasText(storedFingerprint)) {
            return true; // 如果没有存储指纹，允许通过（向后兼容）
        }

        String currentFingerprint = generateFingerprint(request);
        boolean isValid = currentFingerprint.equals(storedFingerprint);

        if (!isValid) {
            log.warn("Token指纹校验失败，IP或User-Agent发生变化");
        }

        return isValid;
    }

    /**
     * 对指纹进行哈希处理
     *
     * @param fingerprint 原始指纹
     * @return 哈希后的指纹
     */
    private String hashFingerprint(String fingerprint) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fingerprint.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("生成指纹哈希失败", e);
            return fingerprint; // 降级返回原始指纹
        }
    }

    /**
     * 检查是否为可信环境（内网IP等）
     *
     * @param request HTTP请求
     * @return 是否可信
     */
    public boolean isTrustedEnvironment(HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);

        // 内网IP段
        return ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("localhost");
    }
}
