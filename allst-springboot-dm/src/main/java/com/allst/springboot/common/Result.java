package com.allst.springboot.common;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 统一响应。所有演示接口都返回它，方便对照文档里的调用结果。
 */
@Getter
@Setter
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.message = "OK";
        result.data = data;
        result.timestamp = Instant.now().toEpochMilli();
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.timestamp = Instant.now().toEpochMilli();
        return result;
    }
}
