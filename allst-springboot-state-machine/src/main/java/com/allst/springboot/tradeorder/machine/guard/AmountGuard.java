package com.allst.springboot.tradeorder.machine.guard;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.GuardChain;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountGuard implements Guard<OrderState, OrderEvent> {

    @Override
    public boolean evaluate(StateContext<OrderState, OrderEvent> context) {
        Object value = GuardChain.variable(context, "amount");
        if (!(value instanceof BigDecimal) || ((BigDecimal) value).signum() <= 0) {
            GuardChain.reject(context, "订单金额必须大于 0");
            return false;
        }
        return true;
    }
}
