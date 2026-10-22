package com.allst.springboot.redis.common;

import lombok.Data;

/**
 * 统一返回体。code 为 0 表示成功，其余与 HTTP 状态含义对齐，方便控制台直接展示。
 */
@Data
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.code = 0;
        result.message = "ok";
        result.data = data;
        return result;
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.code = code;
        result.message = message;
        return result;
    }
}
