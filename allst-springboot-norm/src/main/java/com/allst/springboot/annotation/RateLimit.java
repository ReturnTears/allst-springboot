package com.allst.springboot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 滑动窗口限流。key 为客户端 IP + 方法 + 路径。
 * 对应规范：4 限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内允许的请求数。 */
    int permits();

    /** 窗口长度，秒。 */
    int periodSeconds();
}
