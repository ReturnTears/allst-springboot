package com.allst.boot.collector;

import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 高级收集器：<b>一次遍历完成「分组 + 聚合 + 排序」，替代多次循环和临时容器</b>。
 * <p>
 * 你已经会用 {@code collect(toList())}，这一层是从「转换集合」跨到「统计报表」的关键。
 * 核心是 {@code groupingBy} 的<b>下游收集器</b>可以嵌套，以及 Java 9+ 的
 * {@code flatMapping} / {@code filtering}。
 */
@Component
public class CollectorShowcase {

    /**
     * 多级分组：城市 → 渠道 → 订单列表。
     * <p>
     * {@code groupingBy} 嵌套即可，无需手写两层 Map 的 containsKey 判断。
     * 业务场景：区域运营看板，先看城市再拆渠道。
     */
    public Map<String, Map<String, List<Order>>> byCityThenChannel(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::getCity,
                Collectors.groupingBy(order -> order.getChannel().getLabel())));
    }

    /**
     * 分组求和：城市 → 成交额。{@code groupingBy + reducing} 一次遍历完成。
     * <p>
     * 注意 BigDecimal 没有 summarizing 版本（那个只支持 int/long/double），
     * 金额统计必须用 reducing 并给出单位元 ZERO。
     */
    public Map<String, BigDecimal> revenueByCity(List<Order> orders) {
        return orders.stream()
                .filter(Order::isPaid)
                .collect(Collectors.groupingBy(
                        Order::getCity,
                        Collectors.reducing(BigDecimal.ZERO, Order::amount, BigDecimal::add)));
    }

    /**
     * 分组后只取每组金额最高的一单：{@code groupingBy + maxBy}。
     * <p>
     * 业务场景：找出每个城市的大客户订单。下游 maxBy 返回 Optional，
     * 因为理论上分组可能为空（实际 groupingBy 不会产生空组，但类型上必须处理）。
     */
    public Map<String, Optional<Order>> topOrderByCity(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::getCity,
                Collectors.maxBy(Comparator.comparing(Order::amount))));
    }

    /**
     * 分组 + 映射 + 去重：城市 → 该城市买过的商品名集合。
     * <p>
     * {@code flatMapping}（Java 9+）是关键：先把每单的明细摊平，再 mapping 取名，最后 toSet 去重。
     * 没有 flatMapping 就得先 groupingBy 拿到 List&lt;Order&gt;，再对每组单独 stream 一次。
     */
    public Map<String, Set<String>> productNamesByCity(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::getCity,
                Collectors.flatMapping(
                        order -> order.getItems().stream(),
                        Collectors.mapping(OrderItem::getName, Collectors.toSet()))));
    }

    /**
     * 分组内再过滤：城市 → 该城市的大额订单（≥500）。
     * <p>
     * {@code filtering}（Java 9+）放在下游，好处是<b>即使某城市没有大额单，该城市的键仍然存在</b>
     * （值为空列表）。若先 filter 再 groupingBy，那个城市会整个消失——报表要求「补零」时这点很重要。
     */
    public Map<String, List<Order>> bigOrdersByCityKeepingAllCities(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::getCity,
                Collectors.filtering(
                        order -> order.amount().compareTo(new BigDecimal("500")) >= 0,
                        Collectors.toList())));
    }

    /**
     * 有序分组：用 TreeMap 让城市按字典序排列，便于导出稳定报表。
     * groupingBy 的三参版本可以指定 Map 工厂。
     */
    public Map<String, Long> countByCitySorted(List<Order> orders) {
        return orders.stream().collect(Collectors.groupingBy(
                Order::getCity,
                TreeMap::new,
                Collectors.counting()));
    }

    /**
     * 一次遍历同时算多个指标：{@code teeing}（Java 12+）在 Java 11 不可用，
     * 这里用 {@code collectingAndThen} + 自定义聚合演示等效做法。
     * <p>
     * 业务场景：报表要同时给出「总单数」和「总金额」，遍历两次浪费。
     */
    public String summary(List<Order> orders) {
        return orders.stream().collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    BigDecimal total = list.stream().map(Order::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return "单数=" + list.size() + ", 金额=" + total;
                }));
    }

    /**
     * 去重的正确姿势：按业务键去重而不是 distinct()。
     * <p>
     * {@code distinct()} 依赖 equals/hashCode，订单没重写就按对象引用比，等于没去重。
     * 这里用 toMap 的合并函数「保留先出现的」，再取 values。
     * LinkedHashMap 保证顺序稳定。
     */
    public List<Order> distinctByOrderNo(List<Order> orders) {
        return orders.stream().collect(Collectors.toMap(
                        Order::getOrderNo,
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(Order::getCreatedAt)),
                        LinkedHashMap::new))
                .values().stream().collect(Collectors.toList());
    }

    /**
     * 拼接展示文案：{@code joining} 带前后缀，替代 StringBuilder 手工拼分隔符。
     * 业务场景：把订单号拼成 IN 查询的提示文案或日志摘要。
     */
    public String orderNoSummary(List<Order> orders) {
        return orders.stream()
                .map(Order::getOrderNo)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 数值统计：{@code summarizingInt} 一次拿到 count/sum/min/max/average。
     * 业务场景：商品件数分布分析。注意它只支持 int/long/double，金额要用 reducing。
     */
    public java.util.IntSummaryStatistics quantityStats(List<Order> orders) {
        return orders.stream().collect(Collectors.summarizingInt(Order::totalQuantity));
    }
}