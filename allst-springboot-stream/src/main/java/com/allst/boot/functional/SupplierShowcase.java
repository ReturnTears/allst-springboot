package com.allst.boot.functional;

import com.allst.boot.domain.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Supplier（生产者）的业务价值：<b>把「怎么算」传进去，让调用方决定「要不要算、算几次」</b>。
 * <p>
 * 只用 map/filter 的人往往忽略这一点：Supplier 不是语法糖，它是延迟执行与复用的载体。
 * 四个真实场景：
 * <ol>
 *   <li>{@link #firstNonEmpty} 多级数据源兜底——命中缓存就不查库（省一次 IO）</li>
 *   <li>{@link #memoize} 把昂贵计算包成「只算一次」的供应者</li>
 *   <li>{@link #amountByCity} computeIfAbsent分组容器按需创建，替代 containsKey + put</li>
 *   <li>{@link #generateSerials} generate造测试数据 / 流水号，无限流 + limit</li>
 * </ol>
 */
@Component
public class SupplierShowcase {

    /**
     * 多级数据源兜底：依次尝试每个来源，返回第一个非空结果。
     * <p>
     * 关键在于参数是 {@code Supplier<List<Order>>} 而不是 {@code List<Order>}：
     * 若传列表，调用方必须先把三个来源全查出来（缓存命中也白查两次库）；
     * 传 Supplier 后，{@code or} 的短路特性保证后面的来源根本不会被调用。
     * <p>
     * 这就是 Stream 里 {@code Optional.or(Supplier)} 的实战形态（Java 9+）。
     *
     * @param sources 按优先级排列的数据源，如 本地缓存 → Redis → 数据库
     */
    @SafeVarargs
    public final List<Order> firstNonEmpty(Supplier<List<Order>>... sources) {
        return Stream.of(sources)
                .map(Supplier::get)
                .filter(list -> list != null && !list.isEmpty())
                .findFirst()
                .orElseGet(List::of);
    }

    /**
     * 严格短路版本：用 {@code Optional.or} 串联，前一个有值就完全不碰后面的 Supplier。
     * <p>
     * 与 {@link #firstNonEmpty} 的区别：上面那版靠 Stream 惰性求值同样短路，
     * 这版更显式地表达「兜底链」语义，且能区分「都没有」时返回空 Optional。
     */
    public java.util.Optional<List<Order>> firstNonEmptyStrict(List<Supplier<List<Order>>> sources) {
        return sources.stream()
                .map(Supplier::get)
                .filter(list -> list != null && !list.isEmpty())
                .findFirst();
    }

    /**
     * 记忆化：把任意昂贵计算包成「首次调用才执行、之后复用结果」的 Supplier。
     * <p>
     * 业务场景：一次报表请求里多个指标都要用「当月总成交额」，
     * 不用记忆化就会重复聚合多次。线程安全由 ConcurrentHashMap 保证。
     */
    public <T> Supplier<T> memoize(Supplier<T> delegate) {
        Map<String, T> holder = new ConcurrentHashMap<>(1);
        return () -> holder.computeIfAbsent("value", key -> delegate.get());
    }

    /**
     * 按城市分组累计金额，容器用 computeIfAbsent 按需创建。
     * <p>
     * 对比传统写法：{@code if (!map.containsKey(city)) map.put(city, ZERO);} 再 get 再 put，
     * 三次哈希查找；computeIfAbsent 一次搞定，且意图更明确。
     */
    public Map<String, BigDecimal> amountByCity(List<Order> orders) {
        Map<String, BigDecimal> result = new ConcurrentHashMap<>();
        orders.forEach(order -> result.computeIfAbsent(order.getCity(), key -> BigDecimal.ZERO));
        orders.forEach(order -> result.merge(order.getCity(), order.amount(), BigDecimal::add));
        return result;
    }

    /**
     * 生成流水号：{@code Stream.generate} 是无限流，必须配 {@code limit} 收口。
     * <p>
     * 业务场景：批量造测试订单、预生成一批券码。Supplier 每次被调用产生一个新值，
     * 这与 {@code Stream.iterate}（有种子、按前值推导）的区别在于「无状态依赖」。
     */
    public List<String> generateSerials(String prefix, int count) {
        AtomicInteger seq = new AtomicInteger();
        Supplier<String> supplier = () -> prefix + String.format("%04d", seq.incrementAndGet());
        return Stream.generate(supplier)
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 延迟求值的价值演示：统计成交额时，只有真的有订单才做聚合。
     * <p>
     * {@code orElse} 会<b>立即</b>执行参数（哪怕是常量也要先构造），
     * {@code orElseGet} 只在为空时才调用 Supplier。数据量大或构造昂贵时差别明显。
     */
    public BigDecimal revenueOrFallback(List<Order> orders, Supplier<BigDecimal> fallback) {
        return orders.stream()
                .filter(Order::isPaid)
                .map(Order::amount)
                .reduce(BigDecimal::add)
                .orElseGet(fallback);
    }
}