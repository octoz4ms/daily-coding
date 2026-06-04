package com.example.order.exception;

import lombok.Getter;

@Getter
public class RemoteServiceException extends RuntimeException {

    private final Integer code;

    public RemoteServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
