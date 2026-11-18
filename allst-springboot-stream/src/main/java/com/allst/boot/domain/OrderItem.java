package com.allst.boot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单明细行。订单与明细是一对多，用来演示 flatMap 摊平嵌套集合。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private String sku;
    private String name;
    private String category;
    private int quantity;
    private BigDecimal price;

    /** 行小计。把计算放在领域对象上，Stream 里只需 map(OrderItem::subtotal)。 */
    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}