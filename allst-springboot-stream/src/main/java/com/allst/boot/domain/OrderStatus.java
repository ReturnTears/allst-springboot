package com.allst.boot.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单状态。终态集合由枚举自己算出来，避免业务代码里散落 status == A || status == B。
 */
public enum OrderStatus {

    CREATED("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 终态：不会再流转的状态。用 Stream 一次算好，静态缓存。 */
    private static final Set<OrderStatus> TERMINAL = Arrays.stream(values())
            .filter(s -> s == COMPLETED || s == CANCELLED || s == REFUNDED)
            .collect(Collectors.toUnmodifiableSet());

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** 是否计入成交额：已支付及之后的状态，但排除退款。 */
    public boolean countsAsRevenue() {
        return this == PAID || this == SHIPPED || this == COMPLETED;
    }

    /**
     * 按名称解析，找不到返回空 Optional 而不是抛异常。
     * 演示 Arrays.stream + filter + findFirst 替代 valueOf 的 try-catch。
     */
    public static Optional<OrderStatus> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
}