package com.allst.boot.functional;

import com.allst.boot.domain.Member;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Function（转换）的业务价值：<b>用 compose / andThen 把转换步骤拼成管道，并复用同一套步骤</b>。
 * <p>
 * 你已经在用 {@code map}，但可能没用到这三个进阶点：
 * <ol>
 *   <li>{@link #compose} 组合函数：清洗 → 标准化 → 转视图，每步独立可测</li>
 *   <li>{@link #indexBy} toMap 建索引，把 O(n×m) 嵌套查找降成 O(n+m)</li>
 *   <li>{@link #toViews} 关联两个集合时先建索引再 map，替代逐条查库（N+1）</li>
 * </ol>
 */
@Component
public class FunctionShowcase {

    /**
     * 函数组合：{@code f.andThen(g)} 是先 f 后 g；{@code f.compose(g)} 是先 g 后 f。
     * <p>
     * 业务场景：导出报表前要把订单「补齐会员等级 → 转成视图」。
     * 组合后的函数可以整体传给 map，也可以单独复用其中一步。
     */
    public Function<Order, OrderView> compose(Map<Long, Member> memberIndex) {
        Function<Order, Order> fillLevel = order -> {
            Member member = memberIndex.get(order.getMemberId());
            if (member != null) {
                order.setMemberName(member.getName());
            }
            return order;
        };
        Function<Order, OrderView> toView = order -> OrderView.builder()
                .orderNo(order.getOrderNo())
                .memberName(order.getMemberName())
                .memberLevel(levelOf(memberIndex, order.getMemberId()))
                .channelLabel(order.getChannel() == null ? "" : order.getChannel().getLabel())
                .statusLabel(order.getStatus() == null ? "" : order.getStatus().getLabel())
                .amount(order.amount())
                .quantity(order.totalQuantity())
                .createdAt(order.getCreatedAt())
                .build();
        return fillLevel.andThen(toView);
    }

    private String levelOf(Map<Long, Member> index, Long memberId) {
        Member member = index.get(memberId);
        return member == null ? "UNKNOWN" : member.getLevel();
    }

    /**
     * 建索引：{@code toMap} 把列表变成「主键 → 对象」的 Map。
     * <p>
     * <b>必须给合并函数</b>：不给的话主键重复会抛 IllegalStateException，
     * 这是 toMap 最常见的线上事故。这里用「保留后者」并显式表达意图。
     * <p>
     * 另一个坑：value 为 null 时 toMap 会 NPE（HashMap.merge 的限制），
     * 所以索引的 value 要用对象本身而不是可能为 null 的字段。
     */
    public Map<Long, Member> indexBy(List<Member> members) {
        return members.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        Function.identity(),
                        (existing, replacement) -> replacement));
    }

    /**
     * 跨集合关联：先建索引，再一次 map 完成拼装。
     * <p>
     * 这是替代 N+1 的核心手法：
     * 反模式是 {@code orders.stream().map(o -> memberRepo.findById(o.getMemberId()))}——每条一次查询；
     * 正确做法是<b>一次批量查出会员</b>，建索引，再在内存里关联。
     */
    public List<OrderView> toViews(List<Order> orders, List<Member> members) {
        Map<Long, Member> index = indexBy(members);
        Function<Order, OrderView> convert = compose(index);
        return orders.stream().map(convert).collect(Collectors.toList());
    }

    /**
     * 按订单号建索引并保留先出现的那条，演示合并函数的另一种选择。
     * 业务场景：上游推送可能重复，去重时明确「以第一条为准」。
     */
    public Map<String, Order> indexByOrderNo(List<Order> orders) {
        return orders.stream()
                .collect(Collectors.toMap(
                        Order::getOrderNo,
                        Function.identity(),
                        (existing, replacement) -> existing));
    }
}