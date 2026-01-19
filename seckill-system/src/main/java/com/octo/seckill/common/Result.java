package com.octo.seckill.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 秒杀相关错误码
     */
    public static <T> Result<T> soldOut() {
        return new Result<>(4001, "商品已售罄", null);
    }

    public static <T> Result<T> repeatSeckill() {
        return new Result<>(4002, "您已参与过此次秒杀", null);
    }

    public static <T> Result<T> activityNotStart() {
        return new Result<>(4003, "秒杀活动未开始", null);
    }

    public static <T> Result<T> activityEnded() {
        return new Result<>(4004, "秒杀活动已结束", null);
    }

    public static <T> Result<T> rateLimited() {
        return new Result<>(4005, "访问过于频繁，请稍后再试", null);
    }

    public static <T> Result<T> systemBusy() {
        return new Result<>(5001, "系统繁忙，请稍后再试", null);
    }
}

