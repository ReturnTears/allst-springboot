package com.allst.springboot.tradeorder.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 每个稳定状态允许的事件。页面按钮从这里来，和状态机配置保持同一份语义。
 */
public final class OrderEventCatalog {

    private static final Map<OrderState, List<OrderEvent>> ALLOWED;

    static {
        Map<OrderState, List<OrderEvent>> map = new EnumMap<>(OrderState.class);
        map.put(OrderState.CREATED, List.of(OrderEvent.SUBMIT_PAY, OrderEvent.CANCEL, OrderEvent.TIMEOUT));
        map.put(OrderState.PAYING, List.of(OrderEvent.PAY_SUCCESS, OrderEvent.PAY_FAIL, OrderEvent.TIMEOUT));
        map.put(OrderState.PAY_FAILED, List.of(OrderEvent.SUBMIT_PAY, OrderEvent.CANCEL, OrderEvent.TIMEOUT));
        map.put(OrderState.RISK_REVIEW, List.of(OrderEvent.RISK_PASS, OrderEvent.RISK_REJECT));
        map.put(OrderState.RISK_REJECTED, List.of(OrderEvent.START_REFUND));
        map.put(OrderState.WAIT_ALLOCATE, List.of(OrderEvent.ALLOCATE, OrderEvent.START_REFUND));
        map.put(OrderState.STOCK_SHORT, List.of(OrderEvent.ALLOCATE, OrderEvent.START_REFUND));
        map.put(OrderState.WAIT_SHIP, List.of(OrderEvent.SHIP, OrderEvent.START_REFUND));
        map.put(OrderState.SHIPPED, List.of(OrderEvent.SIGN));
        map.put(OrderState.DELIVERED, List.of(OrderEvent.CONFIRM, OrderEvent.APPLY_RETURN));
        map.put(OrderState.RETURN_APPLIED, List.of(OrderEvent.APPROVE_RETURN, OrderEvent.REJECT_RETURN));
        map.put(OrderState.RETURN_APPROVED, List.of(OrderEvent.START_REFUND));
        map.put(OrderState.RETURN_REJECTED, List.of(OrderEvent.CLOSE_AFTER_SALE));
        map.put(OrderState.REFUNDING, List.of(OrderEvent.REFUND_SUCCESS));
        ALLOWED = Collections.unmodifiableMap(map);
    }

    private OrderEventCatalog() {
    }

    public static List<OrderEvent> allowed(OrderState state) {
        return ALLOWED.getOrDefault(state, List.of());
    }
}
