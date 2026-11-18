package com.allst.boot.support;

import com.allst.boot.domain.Channel;
import com.allst.boot.domain.Member;
import com.allst.boot.domain.Order;
import com.allst.boot.domain.OrderItem;
import com.allst.boot.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 固定的演示数据。所有案例与测试共用同一份，保证结果可预期、可断言。
 * <p>
 * 刻意包含边界情况：空明细订单、无券订单、未支付订单、跨城市、跨渠道，
 * 这样分组/去重/合并函数的问题才会暴露出来。
 */
public final class Orders {

    private Orders() {
    }

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 18, 10, 0);

    public static List<Order> sample() {
        return List.of(
                order("NO-1001", 1L, "张三", Channel.APP, OrderStatus.PAID, "成都", "NEW10", 0,
                        item("SKU-A", "机械键盘", "外设", 1, "299.00"),
                        item("SKU-B", "鼠标垫", "外设", 2, "39.50")),
                order("NO-1002", 2L, "李四", Channel.WEB, OrderStatus.CREATED, "北京", null, 10,
                        item("SKU-C", "显示器", "外设", 1, "1299.00")),
                order("NO-1003", 1L, "张三", Channel.MINI_PROGRAM, OrderStatus.COMPLETED, "成都", "NEW10", 20,
                        item("SKU-A", "机械键盘", "外设", 2, "299.00")),
                order("NO-1004", 3L, "王五", Channel.OFFLINE, OrderStatus.REFUNDED, "成都", null, 30,
                        item("SKU-D", "咖啡豆", "食品", 3, "88.00")),
                order("NO-1005", 2L, "李四", Channel.APP, OrderStatus.SHIPPED, "北京", "VIP20", 40,
                        item("SKU-E", "橄榄油", "食品", 1, "159.90"),
                        item("SKU-D", "咖啡豆", "食品", 1, "88.00")),
                // 空明细订单：验证 amount() 返回 0 而不是 NPE，也验证 flatMap 不会丢订单
                order("NO-1006", 4L, "赵六", Channel.WEB, OrderStatus.CANCELLED, "上海", null, 50)
        );
    }

    public static List<Member> members() {
        return List.of(
                Member.builder().id(1L).name("张三").level("GOLD").city("成都").points(1200).build(),
                Member.builder().id(2L).name("李四").level("SILVER").city("北京").points(300).build(),
                Member.builder().id(3L).name("王五").level("NORMAL").city("成都").points(50).build(),
                Member.builder().id(4L).name("赵六").level("NORMAL").city("上海").points(0).build()
        );
    }

    public static Order order(String no, Long memberId, String memberName, Channel channel,
                              OrderStatus status, String city, String coupon, int minutesLater,
                              OrderItem... items) {
        return Order.builder()
                .orderNo(no)
                .memberId(memberId)
                .memberName(memberName)
                .channel(channel)
                .status(status)
                .city(city)
                .couponCode(coupon)
                .createdAt(BASE.plusMinutes(minutesLater))
                .paidAt(status.countsAsRevenue() ? BASE.plusMinutes(minutesLater + 5) : null)
                .items(Arrays.asList(items))
                .build();
    }

    public static OrderItem item(String sku, String name, String category, int quantity, String price) {
        return OrderItem.builder()
                .sku(sku).name(name).category(category)
                .quantity(quantity).price(new BigDecimal(price))
                .build();
    }
}