package com.octo.eum.security;

/**
 * 登录策略枚举
 *
 * @author octo
 */
public enum LoginPolicy {

    /**
     * 多端同时在线 - 不限制，所有设备可同时登录
     */
    MULTI,

    /**
     * 单设备登录 - 新登录会踢掉所有其他设备
     */
    SINGLE,

    /**
     * 同类型设备互踢 - 同类型设备只能一个在线（如：手机踢手机，电脑踢电脑）
     */
    SAME_TYPE_KICK
}

