package com.allst.boot.functional;

import com.allst.boot.domain.Channel;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Predicate（判断）的业务价值：<b>把筛选条件变成可组合、可复用、可配置的值</b>。
 * <p>
 * 传统写法是 {@code if (a && b && c)} 堆在循环里，新增条件就得改方法（违反开闭原则）。
 * 用 Predicate 后，条件是「数据」：能从配置读、能按渠道拼装、能单测。
 * <p>
 * 三个组合器：{@code and}（与）、{@code or}（或）、{@code negate}（非）。
 * 还有两个静态工厂：{@code isEqual}、{@code not}（Java 11+）。
 */
@Component
public class PredicateShowcase {

    /** 已支付且计入成交额。 */
    public Predicate<Order> paid() {
        return Order::isPaid;
    }

    /** 金额不低于阈值。 */
    public Predicate<Order> amountAtLeast(BigDecimal threshold) {
        return order -> order.amount().compareTo(threshold) >= 0;
    }

    /** 指定城市。 */
    public Predicate<Order> inCity(String city) {
        return order -> city.equals(order.getCity());
    }

    /** 移动端下单。判断收在枚举里，这里只做转发。 */
    public Predicate<Order> mobile() {
        return Order::isMobile;
    }

    /** 创建时间落在区间内（含边界）。 */
    public Predicate<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return order -> order.getCreatedAt() != null
                && !order.getCreatedAt().isBefore(from)
                && !order.getCreatedAt().isAfter(to);
    }

    /**
     * 组合筛选：把一组条件用 and 串起来，一次 filter 完成。
     * <p>
     * {@code reduce} 的单位元是 {@code order -> true}（恒真），
     * 这样空条件列表表示「不过滤」，语义自然，不用特判。
     * <p>
     * 业务场景：后台订单列表的查询条件由前端动态传入，
     * 服务端把每个非空参数转成一个 Predicate 再组合，无需写 if 嵌套。
     */
    public List<Order> filter(List<Order> orders, List<Predicate<Order>> conditions) {
        Predicate<Order> combined = conditions.stream()
                .reduce(order -> true, Predicate::and);
        return orders.stream().filter(combined).collect(Collectors.toList());
    }

    /**
     * 「任一命中」筛选：用 or 组合，单位元是恒假。
     * 业务场景：风控命中任意一条规则就要拦截。
     */
    public List<Order> filterAny(List<Order> orders, List<Predicate<Order>> rules) {
        Predicate<Order> combined = rules.stream()
                .reduce(order -> false, Predicate::or);
        return orders.stream().filter(combined).collect(Collectors.toList());
    }

    /**
     * 分区：{@code partitioningBy} 按一个 Predicate 把数据切成「命中/未命中」两份。
     * <p>
     * 比写两次 filter 更省——只遍历一次。业务场景：批量发券时把「符合条件」和
     * 「不符合条件」分开，后者要回传失败原因。
     */
    public java.util.Map<Boolean, List<Order>> split(List<Order> orders, Predicate<Order> condition) {
        return orders.stream().collect(Collectors.partitioningBy(condition));
    }

    /**
     * 取反：找出「非终态」订单，即仍需跟进的单子。
     * {@code Predicate.not} 是 Java 11 新增，比 lambda 取反更可读。
     */
    public List<Order> pending(List<Order> orders) {
        return orders.stream()
                .filter(Predicate.not(order -> order.getStatus() != null && order.getStatus().isTerminal()))
                .collect(Collectors.toList());
    }

    /** 按渠道与状态精确匹配，演示 Predicate.isEqual 省去 lambda。 */
    public List<Order> ofChannelAndStatus(List<Order> orders, Channel channel, OrderStatus status) {
        Predicate<Order> byChannel = order -> Predicate.isEqual(channel).test(order.getChannel());
        Predicate<Order> byStatus = order -> Predicate.isEqual(status).test(order.getStatus());
        return orders.stream().filter(byChannel.and(byStatus)).collect(Collectors.toList());
    }
}