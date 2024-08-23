package com.allst.springboot.base;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 为了提高代码复用性和减少重复代码，可以设计一个通用的控制器基类，其他具体的控制器类可以继承它。
 * 通用控制器基类可以提供一些常用的方法，如返回成功响应、错误响应等。
 *
 * @author Hutu
 * @since 2024-08-23 下午 10:37
 */
@RestControllerAdvice
public class BaseController {
    protected <T> ResponseEntity<T> success(T data) {
        return ResponseEntity.ok().body(data);
    }

    protected ResponseEntity<?> error(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }
}
