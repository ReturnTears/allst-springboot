package com.allst.springboot.redis.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 固定窗口限流。拦截器按「客户端 IP + 请求路径」计数。
 * 适合演示注解用法；多实例部署时计数在 Redis 中，各节点共享同一限额。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 5;

    int windowSeconds() default 10;
}
