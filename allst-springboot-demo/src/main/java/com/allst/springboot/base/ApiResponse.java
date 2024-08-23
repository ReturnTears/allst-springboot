package com.allst.springboot.base;

import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-23 下午 10:40
 */
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
