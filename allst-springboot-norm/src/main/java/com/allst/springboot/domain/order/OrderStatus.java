package com.allst.springboot.domain.order;

/**
 * 订单状态。取消只能从 CREATED 到 CANCELED，不在创建接口里顺带改状态。
 */
public enum OrderStatus {
    CREATED,
    CANCELED
}
