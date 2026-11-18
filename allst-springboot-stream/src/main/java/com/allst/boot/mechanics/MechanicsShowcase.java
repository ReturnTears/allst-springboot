package com.allst.boot.mechanics;

import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 流机制与陷阱：<b>理解惰性求值、短路、一次性，才能写出既优雅又不出事的代码</b>。
 * <p>
 * 这一层不讲新算子，讲「为什么这样写对、那样写错」。每个方法都对应一个真实踩坑点。
 */
@Component
public class MechanicsShowcase {

    /**
     * 惰性求值 + 短路：{@code findFirst} 之前的 map 只对必要元素执行。
     * <p>
     * 业务价值：从大量订单里找第一个大额单，不需要把全部订单都算一遍金额。
     * 这也解释了为什么 {@code filter} 放在 {@code map} 前面能省计算——
     * 流是「一个元素走完整条管道」，不是「先全部 filter 再全部 map」。
     *
     * @param probe 用于观察实际执行次数的回调
     */
    public Optional<String> firstBigOrderNo(List<Order> orders, java.util.function.Consumer<String> probe) {
        return orders.stream()
                .peek(order -> probe.accept(order.getOrderNo()))
                .filter(order -> order.amount().compareTo(new BigDecimal("500")) >= 0)
                .map(Order::getOrderNo)
                .findFirst();
    }

    /**
     * flatMap 摊平嵌套集合：订单列表 → 所有明细行。
     * <p>
     * 关键区别：{@code map} 会得到 {@code Stream<List<OrderItem>>}（还是嵌套），
     * {@code flatMap} 得到 {@code Stream<OrderItem>}（摊平）。
     * <p>
     * 空明细订单不会导致异常，只是贡献 0 个元素——但订单本身也就「消失」了，
     * 若要保留订单维度信息，就不能只 flatMap，见 {@link #itemsWithOrderNo}。
     */
    public List<OrderItem> allItems(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.toList());
    }

    /**
     * flatMap 保留父级信息：摊平明细的同时带上订单号。
     * <p>
     * 这是 flatMap 最实用的形态——{@code map} 里再套一层 {@code map}，
     * 把父对象的字段「广播」到每个子元素上。业务场景：导出明细报表要带订单号。
     */
    public List<String> itemsWithOrderNo(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getItems().stream()
                        .map(item -> order.getOrderNo() + "/" + item.getName()))
                .collect(Collectors.toList());
    }

    /**
     * takeWhile / dropWhile（Java 9+）：按条件截断，而不是过滤。
     * <p>
     * 与 filter 的本质区别：{@code takeWhile} 遇到第一个不满足就<b>停止</b>，
     * 后面即使有满足的也不要。业务场景：取「连续未支付」的前缀做催付，
     * 遇到已支付就停（按时间排序后）。
     */
    public List<Order> leadingUnpaid(List<Order> sortedByTime) {
        return sortedByTime.stream()
                .takeWhile(order -> !order.isPaid())
                .collect(Collectors.toList());
    }

    /**
     * 排序陷阱：Comparator 链式组合，null 值必须显式处理。
     * <p>
     * {@code comparing(Order::getPaidAt)} 遇到未支付订单（paidAt 为 null）会 NPE。
     * 正确做法是 {@code nullsLast}，并明确「先按金额倒序，再按时间正序」。
     */
    public List<Order> sortByAmountDescThenTime(List<Order> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(Order::amount).reversed()
                        .thenComparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 流是一次性的：同一个 Stream 对象不能消费两次，否则抛 IllegalStateException。
     * <p>
     * 正确做法是<b>每次从集合重新创建流</b>，或者用 Supplier&lt;Stream&gt; 传递「造流的方法」
     * 而不是流本身。这也是 Supplier 的一个真实用途。
     */
    public long countTwiceCorrectly(List<Order> orders) {
        java.util.function.Supplier<Stream<Order>> streamSupplier = () -> orders.stream().filter(Order::isPaid);
        long paid = streamSupplier.get().count();
        long paidAgain = streamSupplier.get().count();
        return paid + paidAgain;
    }

    /**
     * 并行流的适用边界：只有「数据量大 + 单元素计算重 + 无共享状态」才值得。
     * <p>
     * 这里演示<b>安全的并行聚合</b>：reduce 带单位元和结合律运算，可安全并行。
     * 反模式是并行流里往 ArrayList 里 add（线程不安全，结果随机丢数据）。
     * <p>
     * 小集合用并行流通常更慢（线程调度开销大于收益），别默认加 parallel。
     */
    public BigDecimal parallelTotal(List<Order> orders) {
        return orders.parallelStream()
                .map(Order::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 副作用陷阱：在 map 里修改对象是反模式。
     * <p>
     * map 的语义是「转换」，应当返回新值；要改原对象就用 forEach 明确表达副作用。
     * 这里给出正确写法：转换用 map 生成新对象，不碰原数据。
     */
    public List<String> upperCaseNamesWithoutMutation(List<Order> orders) {
        return orders.stream()
                .map(order -> order.getMemberName().toUpperCase())
                .collect(Collectors.toList());
    }

    /**
     * Optional 的正确用法：orElse 与 orElseGet 的求值时机差异。
     * <p>
     * {@code orElse(expensive())} 无论是否为空都会执行 expensive()；
     * {@code orElseGet(() -> expensive())} 只在为空时执行。
     * 这是 Supplier 在 Optional 上的核心价值。
     */
    public String firstCityOrDefault(List<Order> orders, java.util.function.Supplier<String> fallback) {
        return orders.stream()
                .map(Order::getCity)
                .findFirst()
                .orElseGet(fallback);
    }
}