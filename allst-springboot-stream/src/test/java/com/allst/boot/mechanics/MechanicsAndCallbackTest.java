package com.allst.boot.mechanics;

import com.allst.boot.callback.BatchTemplate;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderItem;
import com.allst.boot.support.Orders;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流机制（惰性、短路、一次性）与回调模板的行为验证。
 */
class MechanicsAndCallbackTest {

    private final MechanicsShowcase mechanics = new MechanicsShowcase();
    private final BatchTemplate template = new BatchTemplate();

    /** 短路：findFirst 只处理到第一个命中元素，后面不再计算。 */
    @Test
    void findFirstShortCircuits() {
        List<String> visited = new ArrayList<>();

        Optional<String> found = mechanics.firstBigOrderNo(Orders.sample(), visited::add);

        assertTrue(found.isPresent());
        assertEquals("NO-1002", found.get());
        assertEquals(2, visited.size(), "只应访问前两个订单，不应遍历全部 6 个");
    }

    /** flatMap 摊平嵌套集合，空明细订单贡献 0 个元素。 */
    @Test
    void flatMapFlattensNestedCollections() {
        List<OrderItem> items = mechanics.allItems(Orders.sample());
        assertEquals(7, items.size());
    }

    /** flatMap 保留父级信息：每个明细带上订单号。 */
    @Test
    void flatMapKeepsParentContext() {
        List<String> lines = mechanics.itemsWithOrderNo(Orders.sample());

        assertTrue(lines.contains("NO-1001/机械键盘"));
        assertTrue(lines.contains("NO-1005/橄榄油"));
        assertEquals(7, lines.size());
    }

    /** takeWhile 遇第一个不满足即停止，与 filter 语义不同。 */
    @Test
    void takeWhileStopsAtFirstMismatch() {
        List<Order> sorted = Orders.sample().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .collect(Collectors.toList());

        List<Order> leading = mechanics.leadingUnpaid(sorted);

        // 按时间排序后第一单 NO-1001 就是 PAID，takeWhile 立即停止 → 空列表。
        // 这正是与 filter 的区别：filter 会继续找后面的未支付单，takeWhile 不会。
        assertTrue(leading.isEmpty(), "首单已支付，takeWhile 应立即停止");

        long unpaidTotal = sorted.stream().filter(o -> !o.isPaid()).count();
        assertEquals(3, unpaidTotal, "filter 能找到 3 条未支付，对比可见语义差异");
    }

    /** 排序：null 值用 nullsLast 兜住，不抛 NPE。 */
    @Test
    void sortingHandlesNullsWithoutException() {
        List<Order> sorted = mechanics.sortByAmountDescThenTime(Orders.sample());

        assertEquals(6, sorted.size());
        assertEquals("NO-1002", sorted.get(0).getOrderNo(), "金额最大者在前");
    }

    /** 流一次性：用 Supplier 重新造流即可安全消费多次。 */
    @Test
    void streamSupplierAllowsRepeatedConsumption() {
        long total = mechanics.countTwiceCorrectly(Orders.sample());
        assertEquals(6, total, "3 条计入成交额，两次共 6");
    }

    /** 直接复用同一个 Stream 对象会抛异常——这是必须记住的陷阱。 */
    @Test
    void reusingStreamThrows() {
        java.util.stream.Stream<Order> stream = Orders.sample().stream();
        stream.count();
        assertThrows(IllegalStateException.class, stream::count);
    }

    /** 并行 reduce 结果与串行一致（结合律运算可安全并行）。 */
    @Test
    void parallelReduceMatchesSequential() {
        BigDecimal sequential = Orders.sample().stream()
                .map(Order::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal parallel = mechanics.parallelTotal(Orders.sample());

        assertEquals(0, sequential.compareTo(parallel));
    }

    /** 分批：最后一批不足 batchSize 也能正确收口。 */
    @Test
    void partitionHandlesRemainder() {
        List<Integer> items = List.of(1, 2, 3, 4, 5);
        List<List<Integer>> batches = template.partition(items, 2);

        assertEquals(3, batches.size());
        assertEquals(List.of(1, 2), batches.get(0));
        assertEquals(List.of(5), batches.get(2));
    }

    /** 分批参数非法时快速失败。 */
    @Test
    void partitionRejectsInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> template.partition(List.of(1), 0));
    }

    /** 回调模板：过滤、转换失败、落库成功分别计数。 */
    @Test
    void batchTemplateSeparatesSkippedFailedSucceeded() {
        List<String> saved = new ArrayList<>();

        BatchTemplate.BatchResult result = template.process(
                () -> List.of("1", "2", "bad", "3", ""),
                s -> !s.isBlank(),                       // 空串被 skip
                Integer::parseInt,                        // "bad" 转换失败
                batch -> batch.forEach(v -> saved.add(String.valueOf(v))),
                2);

        assertEquals(3, result.getSucceeded());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getSkipped());
        assertEquals(List.of("1", "2", "3"), saved);
        assertEquals(1, result.getErrors().size());
    }

    /** 空数据源不执行任何回调。 */
    @Test
    void batchTemplateHandlesEmptySource() {
        AtomicInteger saves = new AtomicInteger();
        BatchTemplate.BatchResult result = template.process(
                List::of, s -> true, s -> s, batch -> saves.incrementAndGet(), 10);

        assertEquals(0, result.getTotal());
        assertEquals(0, saves.get());
    }

    /** 重试：前两次失败第三次成功，成功后不再尝试。 */
    @Test
    void retryStopsOnSuccess() {
        AtomicInteger attempts = new AtomicInteger();

        Optional<String> result = template.retry(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("暂时失败");
            }
            return "ok";
        }, 5);

        assertTrue(result.isPresent());
        assertEquals(3, attempts.get(), "成功后不应继续尝试");
    }
}