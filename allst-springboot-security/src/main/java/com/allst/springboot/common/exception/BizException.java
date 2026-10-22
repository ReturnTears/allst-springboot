package com.allst.springboot.common.exception;

import lombok.Getter;

/**
 * 可预期的业务失败。httpStatus 写到响应行，code 写到响应体。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BizException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
