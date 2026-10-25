package com.allst.springboot.tradeorder.machine.guard;

import com.allst.springboot.config.TradeProperties;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.GuardChain;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class HighRiskGuard implements Guard<OrderState, OrderEvent> {

    private final TradeProperties properties;

    public HighRiskGuard(TradeProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean evaluate(StateContext<OrderState, OrderEvent> context) {
        Object value = GuardChain.variable(context, "riskScore");
        int score = value instanceof Number ? ((Number) value).intValue() : 0;
        return score >= properties.getHighRiskScore();
    }
}
