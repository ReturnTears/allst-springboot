package com.allst.springboot.tradeorder.machine.action;

import com.allst.springboot.config.AppClock;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.domain.TradeOrder;
import com.allst.springboot.tradeorder.machine.GuardChain;
import com.allst.springboot.tradeorder.repo.TradeOrderRepository;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class ShipAction implements Action<OrderState, OrderEvent> {

    private final TradeOrderRepository orderRepository;
    private final AppClock clock;

    public ShipAction(TradeOrderRepository orderRepository, AppClock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        String orderNo = context.getStateMachine().getId();
        TradeOrder order = orderRepository.findByOrderNo(orderNo).orElseThrow();
        order.setShipmentNo(GuardChain.header(context, "shipmentNo"));
        order.setShippedAt(clock.now());
    }
}
