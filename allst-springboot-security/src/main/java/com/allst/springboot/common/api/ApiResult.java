package com.allst.springboot.common.api;

import lombok.Getter;

/**
 * 统一返回体。code 为 0 表示成功，其余见 {@link ResultCode}。
 */
@Getter
public class ApiResult<T> {

    private final int code;
    private final String message;
    private final T data;

    private ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(ResultCode.OK, "ok", data);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
