package com.example.order.exception;

import com.example.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RemoteServiceException.class)
    public Result<Void> handleRemoteServiceException(RemoteServiceException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
