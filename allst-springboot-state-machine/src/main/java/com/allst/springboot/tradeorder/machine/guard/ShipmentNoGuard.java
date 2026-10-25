package com.allst.springboot.tradeorder.machine.guard;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.GuardChain;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class ShipmentNoGuard implements Guard<OrderState, OrderEvent> {

    @Override
    public boolean evaluate(StateContext<OrderState, OrderEvent> context) {
        String shipmentNo = GuardChain.header(context, "shipmentNo");
        if (shipmentNo == null || shipmentNo.isBlank()) {
            GuardChain.reject(context, "发货需要物流单号");
            return false;
        }
        return true;
    }
}
