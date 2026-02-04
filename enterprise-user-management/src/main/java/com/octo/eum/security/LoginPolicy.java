package com.octo.eum.security;

/**
 * 登录策略
 *
 * @author octo
 */
public enum LoginPolicy {

    /**
     * 多端共存 - 不限制设备数量
     */
    MULTI,

    /**
     * 单设备 - 只允许一个设备登录，新登录踢掉所有旧设备
     */
    SINGLE,

    /**
     * 同端互踢 - 同类型设备互踢（主流方式）
     * 例：手机A登录后，手机B登录会踢掉手机A，但电脑不受影响
     */
    SAME_TYPE_KICK,

    /**
     * 最大设备数 - 限制最大同时在线设备数
     * 超出时踢掉最早登录的设备
     */
    MAX_DEVICE
}
