package com.allst.springboot.domain;

/**
 * 演示主线用到的订单状态。状态模式的转移表定义在 behavioral.state 包。
 */
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    CLOSED,
    CANCELLED
}
