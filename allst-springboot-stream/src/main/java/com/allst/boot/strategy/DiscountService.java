package com.allst.boot.strategy;

import com.allst.boot.domain.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 折扣编排：演示「策略选择 + Stream 批量计算」的组合。
 * <p>
 * 对比传统写法：
 * <pre>
 * // 反模式：每加一种券就改这个方法
 * if ("NEW10".equals(coupon)) { ... } else if ("VIP20".equals(coupon)) { ... }
 * </pre>
 * 现在选择逻辑收敛到 {@link DiscountStrategy#of}，本类只负责编排，
 * 新增券种<b>不需要改动这里任何一行</b>。
 */
@Service
public class DiscountService {

    /** 单笔应付金额。 */
    public BigDecimal payable(Order order) {
        return DiscountStrategy.of(order.getCouponCode()).apply(order);
    }

    /**
     * 批量计算应付总额：一条管道表达「选策略 → 算金额 → 求和」。
     * <p>
     * reduce 给出单位元 ZERO，空列表自然返回 0，不用特判。
     */
    public BigDecimal totalPayable(List<Order> orders) {
        return orders.stream()
                .map(this::payable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 按券种统计优惠力度：{@code groupingBy + reducing} 一次遍历完成分组聚合。
     * <p>
     * 业务场景：营销活动复盘，要看每种券实际让了多少钱。
     * 优惠额 = 原价 - 应付，用 groupingBy 下游的 reducing 直接算，无需先分组再循环。
     */
    public Map<String, BigDecimal> savingsByCoupon(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getCouponCode() != null)
                .collect(Collectors.groupingBy(
                        Order::getCouponCode,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                order -> order.amount().subtract(payable(order)),
                                BigDecimal::add)));
    }

    /**
     * 券种使用次数排行：{@code groupingBy + counting}，再按次数倒序。
     * <p>
     * 演示「分组统计 → 转 Map 条目流 → 排序 → 收集」这条常见报表管道。
     * LinkedHashMap 保序，否则排好的顺序会被 HashMap 打乱。
     */
    public Map<String, Long> couponRanking(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getCouponCode() != null)
                .collect(Collectors.groupingBy(Order::getCouponCode, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
    }

    /** 把策略表暴露成「券码 → 说明」，供前端下拉框使用。枚举天然可被 Stream 消费。 */
    public Map<String, String> availableCoupons() {
        return java.util.Arrays.stream(DiscountStrategy.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        DiscountStrategy::getDescription,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
    }

    /** 供测试断言：策略解析是纯函数，不依赖 Spring 容器。 */
    public Function<String, DiscountStrategy> resolver() {
        return DiscountStrategy::of;
    }
}