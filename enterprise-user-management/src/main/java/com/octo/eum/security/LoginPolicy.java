package com.octo.eum.security;

/**
 * 登录策略
 *
 * @author octo
 */
public enum LoginPolicy {

    /**
     * 多端共存 - 不限制
     */
    MULTI,

    /**
     * 单设备 - 踢掉所有其他设备
     */
    SINGLE,

    /**
     * 同端互踢 - 同类型设备互踢（主流方式）
     */
    SAME_TYPE_KICK
}
