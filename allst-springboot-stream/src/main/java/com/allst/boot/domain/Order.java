package com.allst.boot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单。贯穿本模块所有 Stream 案例的主模型。
 * <p>
 * 领域方法（{@link #amount()}、{@link #isMobile()}）刻意放在对象上而不是工具类里：
 * Stream 管道里写 {@code map(Order::amount)} 比 {@code map(o -> o.getItems().stream()...)} 干净得多，
 * 这是「让流式代码可读」的前提——先把计算收敛到领域对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderNo;
    private Long memberId;
    private String memberName;
    private Channel channel;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String city;
    private String couponCode;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** 订单金额 = 各行小计之和。reduce 的典型用法：给出单位元 BigDecimal.ZERO。 */
    public BigDecimal amount() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isMobile() {
        return channel != null && channel.isMobile();
    }

    public boolean isPaid() {
        return status != null && status.countsAsRevenue();
    }

    /** 商品总件数。mapToInt + sum 避免装箱，比 map + reduce 更省。 */
    public int totalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public List<OrderItem> getItems() {
        return items == null ? new ArrayList<>() : items;
    }
}