package com.octo.eum.security;

/**
 * 设备类型枚举
 *
 * @author octo
 */
public enum DeviceType {

    /**
     * 手机
     */
    MOBILE("mobile"),

    /**
     * 平板
     */
    TABLET("tablet"),

    /**
     * 电脑
     */
    PC("pc"),

    /**
     * 未知设备
     */
    UNKNOWN("unknown");

    private final String code;

    DeviceType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DeviceType fromCode(String code) {
        for (DeviceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

