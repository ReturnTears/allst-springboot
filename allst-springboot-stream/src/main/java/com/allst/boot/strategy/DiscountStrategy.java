package com.allst.boot.strategy;

import com.allst.boot.domain.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * 函数式策略模式：<b>枚举 + Function 字段</b>，替代「一个接口 + N 个实现类 + 工厂」。
 * <p>
 * 传统策略模式的痛点：每种折扣一个类，十几行样板代码只为包一个算法，
 * 还要写工厂或 if-else 来选择。枚举把「策略标识、算法、说明」收在一处，
 * 新增折扣只加一个枚举常量，选择逻辑完全不用改（开闭原则）。
 * <p>
 * 适用前提：策略<b>无状态、无需注入依赖</b>。若折扣要查数据库或调风控，
 * 就该用 Spring Bean + Map 注入的版本，见 {@link DiscountService}。
 */
public enum DiscountStrategy {

    /** 新人券：立减 10，不低于 0。 */
    NEW10("新人立减10元", order -> order.amount().subtract(BigDecimal.TEN)),

    /** 会员券：八折。 */
    VIP20("会员八折", order -> order.amount().multiply(new BigDecimal("0.8"))),

    /** 满减：满 500 减 50，否则不减。 */
    FULL500("满500减50", order -> {
        BigDecimal amount = order.amount();
        return amount.compareTo(new BigDecimal("500")) >= 0
                ? amount.subtract(new BigDecimal("50"))
                : amount;
    }),

    /** 无券：原价。单位元策略，避免调用方判空。 */
    NONE("无优惠", Order::amount);

    private final String description;
    private final Function<Order, BigDecimal> calculator;

    DiscountStrategy(String description, Function<Order, BigDecimal> calculator) {
        this.description = description;
        this.calculator = calculator;
    }

    public String getDescription() {
        return description;
    }

    /** 执行策略并统一处理精度与下限，避免每个策略各写一遍 setScale。 */
    public BigDecimal apply(Order order) {
        BigDecimal result = calculator.apply(order);
        return result.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按券码解析策略，未知券码回落到 NONE 而不是抛异常。
     * <p>
     * 用 {@code Arrays.stream + filter + findFirst + orElse} 替代 switch，
     * 新增策略时这段代码不用动。
     */
    public static DiscountStrategy of(String couponCode) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(couponCode == null ? "" : couponCode.trim()))
                .findFirst()
                .orElse(NONE);
    }

    /** 供上层展示可用券列表，演示枚举也能直接被 Stream 消费。 */
    public static Optional<DiscountStrategy> find(String couponCode) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(couponCode == null ? "" : couponCode.trim()))
                .findFirst();
    }
}