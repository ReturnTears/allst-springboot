package com.allst.boot.functional;

import com.allst.boot.domain.Member;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderView;
import com.allst.boot.support.Orders;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 四大函数式接口的行为验证。每个断言都对应文档里的一条「好处」。
 */
class FunctionalInterfaceTest {

    private final SupplierShowcase suppliers = new SupplierShowcase();
    private final ConsumerShowcase consumers = new ConsumerShowcase();
    private final FunctionShowcase functions = new FunctionShowcase();
    private final PredicateShowcase predicates = new PredicateShowcase();

    /** Supplier 的核心价值：命中第一个来源后，后面的来源根本不被调用（省 IO）。 */
    @Test
    void supplierShortCircuitsLaterSources() {
        AtomicInteger cacheCalls = new AtomicInteger();
        AtomicInteger dbCalls = new AtomicInteger();

        Supplier<List<Order>> cache = () -> {
            cacheCalls.incrementAndGet();
            return Orders.sample();
        };
        Supplier<List<Order>> database = () -> {
            dbCalls.incrementAndGet();
            return Orders.sample();
        };

        List<Order> result = suppliers.firstNonEmpty(cache, database);

        assertFalse(result.isEmpty());
        assertEquals(1, cacheCalls.get());
        assertEquals(0, dbCalls.get(), "缓存命中后不应再查库");
    }

    /** 缓存为空时才回落到下一个来源。 */
    @Test
    void supplierFallsBackWhenEmpty() {
        AtomicInteger dbCalls = new AtomicInteger();
        Supplier<List<Order>> empty = List::of;
        Supplier<List<Order>> database = () -> {
            dbCalls.incrementAndGet();
            return Orders.sample();
        };

        List<Order> result = suppliers.firstNonEmpty(empty, database);

        assertEquals(6, result.size());
        assertEquals(1, dbCalls.get());
    }

    /** 记忆化：昂贵计算只执行一次。 */
    @Test
    void memoizeComputesOnce() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<BigDecimal> expensive = () -> {
            calls.incrementAndGet();
            return new BigDecimal("100.00");
        };

        Supplier<BigDecimal> cached = suppliers.memoize(expensive);
        cached.get();
        cached.get();
        cached.get();

        assertEquals(1, calls.get(), "多次取值只应计算一次");
    }

    /** orElseGet 只在为空时才调用兜底 Supplier。 */
    @Test
    void orElseGetIsLazy() {
        AtomicInteger fallbackCalls = new AtomicInteger();
        Supplier<BigDecimal> fallback = () -> {
            fallbackCalls.incrementAndGet();
            return BigDecimal.ZERO;
        };

        BigDecimal paid = suppliers.revenueOrFallback(Orders.sample(), fallback);
        assertTrue(paid.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, fallbackCalls.get(), "有值时不应调用兜底");

        suppliers.revenueOrFallback(List.of(), fallback);
        assertEquals(1, fallbackCalls.get(), "为空时才调用兜底");
    }

    /** Stream.generate 是无限流，limit 必须收口。 */
    @Test
    void generateSerialsWithLimit() {
        List<String> serials = suppliers.generateSerials("SN-", 3);
        assertEquals(List.of("SN-0001", "SN-0002", "SN-0003"), serials);
    }

    /** computeIfAbsent + merge 完成分组累计。 */
    @Test
    void amountByCityUsesComputeIfAbsent() {
        Map<String, BigDecimal> result = suppliers.amountByCity(Orders.sample());
        assertTrue(result.containsKey("成都"));
        assertTrue(result.containsKey("北京"));
        assertTrue(result.get("成都").compareTo(BigDecimal.ZERO) > 0);
    }

    /** Consumer.andThen：一次遍历完成多步加工。 */
    @Test
    void consumerAndThenAppliesAllSteps() {
        List<Order> orders = new ArrayList<>(Orders.sample());
        List<Consumer<Order>> steps = List.of(consumers.maskMemberName(), consumers.clearCoupon());

        consumers.enrich(orders, steps);

        assertEquals("张*", orders.get(0).getMemberName());
        assertNull(orders.get(0).getCouponCode());
    }

    /** 批处理单条失败不中断整批，并返回失败数。 */
    @Test
    void forEachSafelyIsolatesFailures() {
        List<String> items = List.of("ok-1", "bad", "ok-2");
        List<String> processed = new ArrayList<>();

        int failures = consumers.forEachSafely(items, item -> {
            if ("bad".equals(item)) {
                throw new IllegalStateException("脏数据");
            }
            processed.add(item);
        });

        assertEquals(1, failures);
        assertEquals(List.of("ok-1", "ok-2"), processed);
    }

    /** toMap 建索引后关联，会员等级正确补齐。 */
    @Test
    void toViewsJoinsViaIndex() {
        List<OrderView> views = functions.toViews(Orders.sample(), Orders.members());

        assertEquals(6, views.size());
        assertEquals("GOLD", views.get(0).getMemberLevel());
        assertEquals("SILVER", views.get(1).getMemberLevel());
    }

    /** toMap 遇重复键时合并函数生效，不抛异常。 */
    @Test
    void indexByHandlesDuplicateKeys() {
        Member first = Member.builder().id(1L).name("旧").level("NORMAL").city("成都").points(0).build();
        Member second = Member.builder().id(1L).name("新").level("GOLD").city("成都").points(10).build();

        Map<Long, Member> index = functions.indexBy(List.of(first, second));

        assertEquals(1, index.size());
        assertEquals("新", index.get(1L).getName(), "应保留后者");
    }

    /** Predicate 组合：多条件 and 串联。 */
    @Test
    void predicateCombinesWithAnd() {
        List<Predicate<Order>> conditions = List.of(
                predicates.inCity("成都"),
                predicates.paid(),
                predicates.amountAtLeast(new BigDecimal("300")));

        List<Order> result = predicates.filter(Orders.sample(), conditions);

        // 成都且计入成交额且金额≥300：NO-1001(378.00)、NO-1003(598.00)
        // NO-1004 虽在成都但已退款，不计入成交额，被 paid() 排除
        assertEquals(2, result.size());
        assertEquals(List.of("NO-1001", "NO-1003"),
                result.stream().map(Order::getOrderNo).collect(java.util.stream.Collectors.toList()));
    }

    /** 空条件列表表示不过滤（单位元恒真）。 */
    @Test
    void emptyConditionsMeansNoFilter() {
        assertEquals(6, predicates.filter(Orders.sample(), List.of()).size());
    }

    /** partitioningBy 一次遍历切成两份。 */
    @Test
    void splitPartitionsInOnePass() {
        Map<Boolean, List<Order>> split = predicates.split(Orders.sample(), predicates.mobile());

        assertEquals(3, split.get(true).size());
        assertEquals(3, split.get(false).size());
    }

    /** Predicate.not 找出非终态订单。 */
    @Test
    void pendingExcludesTerminalStatus() {
        List<Order> pending = predicates.pending(Orders.sample());
        assertEquals(3, pending.size());
        assertTrue(pending.stream().noneMatch(o -> o.getStatus().isTerminal()));
    }
}