package com.octo.eum.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码枚举
 *
 * @author octo
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(422, "参数校验失败"),

    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    USER_LOCKED(1005, "用户已被锁定"),
    PASSWORD_ERROR(1006, "密码错误"),
    OLD_PASSWORD_ERROR(1007, "原密码错误"),

    // 角色权限错误 2xxx
    ROLE_NOT_FOUND(2001, "角色不存在"),
    ROLE_ALREADY_EXISTS(2002, "角色已存在"),
    ROLE_IN_USE(2003, "角色正在使用中，无法删除"),
    PERMISSION_NOT_FOUND(2004, "权限不存在"),
    PERMISSION_ALREADY_EXISTS(2005, "权限已存在"),

    // Token错误 3xxx
    TOKEN_INVALID(3001, "Token无效"),
    TOKEN_EXPIRED(3002, "Token已过期"),
    TOKEN_REQUIRED(3003, "请提供Token"),
    REFRESH_TOKEN_INVALID(3004, "RefreshToken无效或已使用"),
    ACCESS_TOKEN_INVALID(3006, "AccessToken无效");

    private final int code;
    private final String message;
}
