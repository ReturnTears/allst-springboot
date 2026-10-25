package com.allst.springboot.tradeorder.machine;

import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;

import java.util.Collection;

public final class OrderStates {

    private OrderStates() {
    }

    /**
     * 层级状态里，当前状态的 id 集合同时包含父状态和子状态。业务上要落库的是那个非结构性状态。
     */
    public static OrderState leaf(StateMachine<OrderState, OrderEvent> machine) {
        State<OrderState, OrderEvent> state = machine.getState();
        if (state == null) {
            throw new BizException("状态机没有当前状态");
        }
        Collection<OrderState> ids = state.getIds();
        OrderState businessState = null;
        for (OrderState id : ids) {
            if (!id.isStructural()) {
                businessState = id;
            }
        }
        return businessState == null ? state.getId() : businessState;
    }
}
