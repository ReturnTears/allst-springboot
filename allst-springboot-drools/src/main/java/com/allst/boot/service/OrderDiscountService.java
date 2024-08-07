package com.allst.boot.service;

import com.allst.boot.model.OrderBO;
import com.allst.boot.model.OrderDiscount;

/**
 * @author Hutu
 * @since 2024-08-07 下午 10:10
 */
public interface OrderDiscountService {
    OrderDiscount getDiscount(OrderBO orderBO);
}
