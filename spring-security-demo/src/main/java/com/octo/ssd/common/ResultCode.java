package com.octo.ssd.common;

import lombok.Getter;

/**
 * 统一响应状态码枚举
 *
 * @author octo
 */
@Getter
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有相关权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),

    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    TOKEN_INVALID(1005, "Token无效或已过期"),
    ROLE_NOT_FOUND(1006, "角色不存在"),
    PERMISSION_NOT_FOUND(1007, "权限不存在"),
    ROLE_ALREADY_EXISTS(1008, "角色编码已存在"),
    PERMISSION_ALREADY_EXISTS(1009, "权限编码已存在"),

    // 参数校验错误 2xxx
    PARAM_ERROR(2001, "参数校验失败"),
    PARAM_MISSING(2002, "必填参数缺失");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}


