package com.allst.boot.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对外订单视图。演示 map 做「领域对象 → DTO」的转换，以及组装时补齐会员等级。
 * 不可变（只有 getter），避免视图被下游误改。
 */
@Getter
@Builder
@AllArgsConstructor
public class OrderView {

    private final String orderNo;
    private final String memberName;
    private final String memberLevel;
    private final String channelLabel;
    private final String statusLabel;
    private final BigDecimal amount;
    private final int quantity;
    private final LocalDateTime createdAt;
}