package com.allst.boot.strategy;

import com.allst.boot.collector.CollectorShowcase;
import com.allst.boot.domain.Order;
import com.allst.boot.support.Orders;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 函数式策略与高级收集器的行为验证。
 */
class StrategyAndCollectorTest {

    private final DiscountService discounts = new DiscountService();
    private final CollectorShowcase collectors = new CollectorShowcase();

    /** 枚举策略：券码解析 + 计算，未知券回落原价。 */
    @Test
    void enumStrategyResolvesAndApplies() {
        Order order = Orders.sample().get(0); // NO-1001, NEW10, 299 + 39.5*2 = 378.00

        assertEquals(new BigDecimal("378.00"), order.amount());
        assertEquals(new BigDecimal("368.00"), discounts.payable(order));
        assertEquals(DiscountStrategy.NEW10, DiscountStrategy.of("NEW10"));
        assertEquals(DiscountStrategy.NONE, DiscountStrategy.of("UNKNOWN-CODE"));
        assertEquals(DiscountStrategy.NONE, DiscountStrategy.of(null));
    }

    /** 满减策略的边界：刚好满 500 与不足 500。 */
    @Test
    void fullReductionBoundary() {
        Order below = Orders.order("T-1", 1L, "测试", com.allst.boot.domain.Channel.APP,
                com.allst.boot.domain.OrderStatus.PAID, "成都", "FULL500", 0,
                Orders.item("S", "商品", "类目", 1, "499.00"));
        Order exact = Orders.order("T-2", 1L, "测试", com.allst.boot.domain.Channel.APP,
                com.allst.boot.domain.OrderStatus.PAID, "成都", "FULL500", 0,
                Orders.item("S", "商品", "类目", 1, "500.00"));

        assertEquals(new BigDecimal("499.00"), DiscountStrategy.FULL500.apply(below));
        assertEquals(new BigDecimal("450.00"), DiscountStrategy.FULL500.apply(exact));
    }

    /** 折扣不会算成负数（下限保护）。 */
    @Test
    void discountNeverNegative() {
        Order tiny = Orders.order("T-3", 1L, "测试", com.allst.boot.domain.Channel.APP,
                com.allst.boot.domain.OrderStatus.PAID, "成都", "NEW10", 0,
                Orders.item("S", "商品", "类目", 1, "5.00"));

        assertEquals(new BigDecimal("0.00"), DiscountStrategy.NEW10.apply(tiny));
    }

    /** groupingBy + reducing：按券种统计优惠额。 */
    @Test
    void savingsByCoupon() {
        Map<String, BigDecimal> savings = discounts.savingsByCoupon(Orders.sample());

        assertEquals(new BigDecimal("20.00"), savings.get("NEW10")); // 两单各减 10
        assertTrue(savings.get("VIP20").compareTo(BigDecimal.ZERO) > 0);
    }

    /** 券种排行按次数倒序，且 LinkedHashMap 保序。 */
    @Test
    void couponRankingIsOrdered() {
        Map<String, Long> ranking = discounts.couponRanking(Orders.sample());
        List<String> keys = List.copyOf(ranking.keySet());

        assertEquals("NEW10", keys.get(0), "使用两次应排第一");
        assertEquals(2L, ranking.get("NEW10"));
    }

    /** 多级分组：城市 → 渠道。 */
    @Test
    void nestedGrouping() {
        Map<String, Map<String, List<Order>>> grouped = collectors.byCityThenChannel(Orders.sample());

        assertTrue(grouped.containsKey("成都"));
        assertTrue(grouped.get("成都").containsKey("移动端"));
        assertEquals(3, grouped.get("成都").values().stream().mapToInt(List::size).sum());
    }

    /** flatMapping：城市 → 商品名去重集合。 */
    @Test
    void flatMappingCollectsProductNames() {
        Map<String, Set<String>> names = collectors.productNamesByCity(Orders.sample());

        assertTrue(names.get("成都").contains("机械键盘"));
        assertTrue(names.get("成都").contains("咖啡豆"));
        assertTrue(names.get("北京").contains("显示器"));
    }

    /** filtering 放下游：没有大额单的城市仍保留键（报表补零场景）。 */
    @Test
    void downstreamFilteringKeepsAllKeys() {
        Map<String, List<Order>> big = collectors.bigOrdersByCityKeepingAllCities(Orders.sample());

        assertTrue(big.containsKey("上海"), "上海只有空明细订单，仍应保留键");
        assertTrue(big.get("上海").isEmpty());
        assertFalse(big.get("北京").isEmpty());
    }

    /** TreeMap 分组：城市按字典序。 */
    @Test
    void treeMapGroupingIsSorted() {
        Map<String, Long> counts = collectors.countByCitySorted(Orders.sample());
        List<String> keys = List.copyOf(counts.keySet());

        assertEquals(List.of("上海", "北京", "成都"), keys);
    }

    /** 按业务键去重，保留创建时间最新的那条。 */
    @Test
    void distinctByBusinessKey() {
        Order older = Orders.order("DUP", 1L, "旧", com.allst.boot.domain.Channel.APP,
                com.allst.boot.domain.OrderStatus.PAID, "成都", null, 0,
                Orders.item("S", "商品", "类目", 1, "10.00"));
        Order newer = Orders.order("DUP", 1L, "新", com.allst.boot.domain.Channel.APP,
                com.allst.boot.domain.OrderStatus.PAID, "成都", null, 60,
                Orders.item("S", "商品", "类目", 1, "20.00"));

        List<Order> result = collectors.distinctByOrderNo(List.of(older, newer));

        assertEquals(1, result.size());
        assertEquals("新", result.get(0).getMemberName());
    }

    /** summarizingInt 一次拿到多个统计量。 */
    @Test
    void summarizingStatistics() {
        java.util.IntSummaryStatistics stats = collectors.quantityStats(Orders.sample());

        assertEquals(6, stats.getCount());
        assertEquals(11, stats.getSum());
        assertEquals(0, stats.getMin());
    }

    /** joining 拼接带前后缀。 */
    @Test
    void joiningWithPrefixSuffix() {
        String summary = collectors.orderNoSummary(Orders.sample());
        assertTrue(summary.startsWith("["));
        assertTrue(summary.endsWith("]"));
        assertTrue(summary.contains("NO-1001, NO-1002"));
    }
}