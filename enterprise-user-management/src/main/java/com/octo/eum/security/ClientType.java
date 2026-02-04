package com.octo.eum.security;

/**
 * 客户端类型（用于同端互斥）
 *
 * @author octo
 */
public enum ClientType {

    PC("pc"),
    MOBILE("mobile"),
    TABLET("tablet"),
    MINI("mini"),
    UNKNOWN("unknown");

    private final String code;

    ClientType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ClientType fromCode(String code) {
        if (code == null) return UNKNOWN;
        for (ClientType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static ClientType fromUserAgent(String ua) {
        if (ua == null) return UNKNOWN;
        ua = ua.toLowerCase();
        
        if (ua.contains("miniprogram") || ua.contains("micromessenger")) {
            return MINI;
        }
        if (ua.contains("ipad") || (ua.contains("android") && !ua.contains("mobile"))) {
            return TABLET;
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")) {
            return MOBILE;
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return PC;
        }
        return UNKNOWN;
    }
}

