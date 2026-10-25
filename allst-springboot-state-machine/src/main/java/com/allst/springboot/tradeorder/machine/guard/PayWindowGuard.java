package com.allst.springboot.tradeorder.machine.guard;

import com.allst.springboot.config.AppClock;
import com.allst.springboot.config.TradeProperties;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.GuardChain;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PayWindowGuard implements Guard<OrderState, OrderEvent> {

    private final TradeProperties properties;
    private final AppClock clock;

    public PayWindowGuard(TradeProperties properties, AppClock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean evaluate(StateContext<OrderState, OrderEvent> context) {
        Object value = GuardChain.variable(context, "createdAt");
        if (!(value instanceof LocalDateTime)) {
            GuardChain.reject(context, "缺少下单时间");
            return false;
        }
        LocalDateTime deadline = ((LocalDateTime) value).plusMinutes(properties.getPayTimeoutMinutes());
        if (clock.now().isAfter(deadline)) {
            GuardChain.reject(context, "已超过支付时限");
            return false;
        }
        return true;
    }
}
