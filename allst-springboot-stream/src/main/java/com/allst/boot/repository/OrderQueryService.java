package com.allst.boot.repository;

import com.allst.boot.callback.BatchTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库 + Stream 实战：<b>用「批量查 + 建索引 + 内存关联」替代 N+1 查询</b>。
 * <p>
 * 这是 Stream 在企业项目里最有价值的应用之一，比集合转换的收益大得多。
 * <p>
 * 反模式（每条订单查一次会员）：
 * <pre>
 * orders.stream().map(o -&gt; {
 *     Member m = memberRepo.findById(o.getMemberId()).orElseThrow(); // N 次 SQL！
 *     return toView(o, m);
 * })
 * </pre>
 * 正确做法见 {@link #listWithMemberLevel}：<b>2 次 SQL</b> 搞定，与订单数量无关。
 */
@Slf4j
@Service
public class OrderQueryService {

    private final OrderRepository orders;
    private final MemberRepository members;
    private final BatchTemplate batchTemplate;

    public OrderQueryService(OrderRepository orders, MemberRepository members, BatchTemplate batchTemplate) {
        this.orders = orders;
        this.members = members;
        this.batchTemplate = batchTemplate;
    }

    /** 视图：订单号 + 会员名 + 等级 + 金额。 */
    public static class OrderWithMember {
        private final String orderNo;
        private final String memberName;
        private final String memberLevel;
        private final BigDecimal amount;

        OrderWithMember(String orderNo, String memberName, String memberLevel, BigDecimal amount) {
            this.orderNo = orderNo;
            this.memberName = memberName;
            this.memberLevel = memberLevel;
            this.amount = amount;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getMemberName() {
            return memberName;
        }

        public String getMemberLevel() {
            return memberLevel;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    /**
     * 核心案例：2 次 SQL 完成关联。
     * <ol>
     *   <li>查订单（1 次 SQL）</li>
     *   <li>用 Stream 提取去重的 memberId 集合，一次 IN 查会员（1 次 SQL）</li>
     *   <li>toMap 建索引，再 map 拼装（纯内存，0 次 SQL）</li>
     * </ol>
     * {@code map(...).collect(toSet())} 这一步就是「批量参数准备」，
     * 去重很重要——1000 条订单可能只涉及 50 个会员。
     */
    @Transactional(readOnly = true)
    public List<OrderWithMember> listWithMemberLevel(String city) {
        List<OrderEntity> orderList = orders.findByCity(city);
        if (orderList.isEmpty()) {
            return List.of();
        }

        Set<Long> memberIds = orderList.stream()
                .map(OrderEntity::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, MemberEntity> memberIndex = members.findByIdIn(memberIds).stream()
                .collect(Collectors.toMap(MemberEntity::getId, Function.identity(), (a, b) -> a));

        return orderList.stream()
                .map(order -> {
                    MemberEntity member = memberIndex.get(order.getMemberId());
                    return new OrderWithMember(
                            order.getOrderNo(),
                            member == null ? "未知会员" : member.getName(),
                            member == null ? "UNKNOWN" : member.getLevel(),
                            order.getAmount());
                })
                .collect(Collectors.toList());
    }

    /**
     * 分组统计落库数据：城市 → 成交额。
     * <p>
     * 说明：真实项目里这种聚合<b>应该交给 SQL 的 GROUP BY</b>（数据库更擅长）。
     * 这里用 Stream 是为了演示语法；何时该用 SQL、何时该用 Stream，见文档的取舍章节。
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> revenueByCity() {
        return orders.findAll().stream()
                .collect(Collectors.groupingBy(
                        OrderEntity::getCity,
                        Collectors.reducing(BigDecimal.ZERO, OrderEntity::getAmount, BigDecimal::add)));
    }

    /**
     * 批量写入：用回调模板分批 insert，避免逐条 save 的 N 次往返。
     * <p>
     * {@code saveAll} 每批一次，配合模板的失败隔离，单批出错不影响其他批。
     */
    @Transactional
    public BatchTemplate.BatchResult importOrders(List<OrderEntity> incoming, int batchSize) {
        return batchTemplate.process(
                () -> incoming,
                order -> order.getOrderNo() != null && order.getAmount() != null,
                Function.identity(),
                orders::saveAll,
                batchSize);
    }

    /**
     * 增量同步：只保存「库里不存在」的订单。
     * <p>
     * 先一次查出已存在的订单号建 Set，再用 filter 排除——
     * 而不是每条都 existsByOrderNo 查一次（那又是 N+1）。
     */
    @Transactional
    public int syncNewOrders(List<OrderEntity> incoming) {
        Set<String> existing = orders.findAll().stream()
                .map(OrderEntity::getOrderNo)
                .collect(Collectors.toSet());

        List<OrderEntity> fresh = incoming.stream()
                .filter(order -> !existing.contains(order.getOrderNo()))
                .collect(Collectors.toList());

        orders.saveAll(fresh);
        return fresh.size();
    }
}