package com.allst.springboot.showcase;

import com.allst.springboot.domain.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单主线的返回：既有业务结果，也带上经过哪些模式的 trace，方便对照文档。
 */
@Getter
@Builder
public class PlaceOrderResult {

    private final String orderNo;
    private final OrderStatus status;
    private final BigDecimal originalAmount;
    private final BigDecimal discount;
    private final BigDecimal payable;
    private final String strategy;
    private final String payChannel;
    private final String paySerialNo;
    private final String compliance;
    private final String waybillNo;
    private final List<String> trace;
    private final boolean duplicate;
}
