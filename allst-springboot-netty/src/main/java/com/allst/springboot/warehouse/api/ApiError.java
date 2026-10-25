package com.allst.springboot.warehouse.api;

/**
 * HTTP 错误体。code 给程序看，message 给看板提示看。
 */
public record ApiError(String code, String message, long timestamp) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, System.currentTimeMillis());
    }
}
