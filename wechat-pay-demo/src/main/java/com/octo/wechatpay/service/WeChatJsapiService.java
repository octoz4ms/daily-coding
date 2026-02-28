package com.octo.wechatpay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octo.wechatpay.config.WeChatPayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 微信 JSAPI 相关服务 - 获取 access_token、jsapi_ticket、生成 wx.config 签名
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatJsapiService {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getjsapi_ticket?access_token=%s";

    private final WeChatPayProperties weChatPayProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<Long> accessTokenExpireAt = new AtomicReference<>(0L);
    private final AtomicReference<String> jsapiTicket = new AtomicReference<>();
    private final AtomicReference<Long> jsapiTicketExpireAt = new AtomicReference<>(0L);

    /**
     * 获取 wx.config 所需参数
     */
    public Map<String, Object> getJsapiConfig(String url) {
        String ticket = getJsapiTicket();
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis() / 1000;

        String str = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp + "&url=" + url;
        String signature = sha1(str);

        Map<String, Object> result = new HashMap<>();
        result.put("appId", weChatPayProperties.getAppId());
        result.put("timestamp", timestamp);
        result.put("nonceStr", nonceStr);
        result.put("signature", signature);
        return result;
    }

    private String getJsapiTicket() {
        long now = System.currentTimeMillis();
        if (jsapiTicket.get() != null && jsapiTicketExpireAt.get() > now + 300_000) {
            return jsapiTicket.get();
        }
        synchronized (this) {
            if (jsapiTicket.get() != null && jsapiTicketExpireAt.get() > now + 300_000) {
                return jsapiTicket.get();
            }
            String token = getAccessToken();
            String resp = restTemplate.getForObject(String.format(TICKET_URL, token), String.class);
            try {
                JsonNode node = objectMapper.readTree(resp);
                if (node.has("ticket")) {
                    jsapiTicket.set(node.get("ticket").asText());
                    int expiresIn = node.has("expires_in") ? node.get("expires_in").asInt() : 7200;
                    jsapiTicketExpireAt.set(now + expiresIn * 1000L);
                    return jsapiTicket.get();
                }
                throw new IllegalStateException("获取 jsapi_ticket 失败: " + resp);
            } catch (Exception e) {
                throw new RuntimeException("解析 jsapi_ticket 失败", e);
            }
        }
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (accessToken.get() != null && accessTokenExpireAt.get() > now + 300_000) {
            return accessToken.get();
        }
        synchronized (this) {
            if (accessToken.get() != null && accessTokenExpireAt.get() > now + 300_000) {
                return accessToken.get();
            }
            String url = String.format(TOKEN_URL, weChatPayProperties.getAppId(), weChatPayProperties.getAppSecret());
            String resp = restTemplate.getForObject(url, String.class);
            try {
                JsonNode node = objectMapper.readTree(resp);
                if (node.has("access_token")) {
                    accessToken.set(node.get("access_token").asText());
                    int expiresIn = node.has("expires_in") ? node.get("expires_in").asInt() : 7200;
                    accessTokenExpireAt.set(now + expiresIn * 1000L);
                    return accessToken.get();
                }
                throw new IllegalStateException("获取 access_token 失败: " + resp);
            } catch (Exception e) {
                throw new RuntimeException("解析 access_token 失败", e);
            }
        }
    }

    private static String sha1(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA1 计算失败", e);
        }
    }
}
