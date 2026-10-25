package com.allst.springboot.tradeorder.machine;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import com.allst.springboot.tradeorder.machine.action.MarkPaidAction;
import com.allst.springboot.tradeorder.machine.action.NotifyCompletedAction;
import com.allst.springboot.tradeorder.machine.action.NotifyRefundAction;
import com.allst.springboot.tradeorder.machine.action.NotifyWarehouseAction;
import com.allst.springboot.tradeorder.machine.action.ShipAction;
import com.allst.springboot.tradeorder.machine.guard.AmountGuard;
import com.allst.springboot.tradeorder.machine.guard.HighRiskGuard;
import com.allst.springboot.tradeorder.machine.guard.PayWindowGuard;
import com.allst.springboot.tradeorder.machine.guard.ReserveStockGuard;
import com.allst.springboot.tradeorder.machine.guard.ShipmentNoGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import static com.allst.springboot.tradeorder.domain.OrderEvent.ALLOCATE;
import static com.allst.springboot.tradeorder.domain.OrderEvent.APPLY_RETURN;
import static com.allst.springboot.tradeorder.domain.OrderEvent.APPROVE_RETURN;
import static com.allst.springboot.tradeorder.domain.OrderEvent.CANCEL;
import static com.allst.springboot.tradeorder.domain.OrderEvent.CLOSE_AFTER_SALE;
import static com.allst.springboot.tradeorder.domain.OrderEvent.CONFIRM;
import static com.allst.springboot.tradeorder.domain.OrderEvent.PAY_FAIL;
import static com.allst.springboot.tradeorder.domain.OrderEvent.PAY_SUCCESS;
import static com.allst.springboot.tradeorder.domain.OrderEvent.REFUND_SUCCESS;
import static com.allst.springboot.tradeorder.domain.OrderEvent.REJECT_RETURN;
import static com.allst.springboot.tradeorder.domain.OrderEvent.RISK_PASS;
import static com.allst.springboot.tradeorder.domain.OrderEvent.RISK_REJECT;
import static com.allst.springboot.tradeorder.domain.OrderEvent.SHIP;
import static com.allst.springboot.tradeorder.domain.OrderEvent.SIGN;
import static com.allst.springboot.tradeorder.domain.OrderEvent.START_REFUND;
import static com.allst.springboot.tradeorder.domain.OrderEvent.SUBMIT_PAY;
import static com.allst.springboot.tradeorder.domain.OrderEvent.TIMEOUT;
import static com.allst.springboot.tradeorder.domain.OrderState.AFTER_SALE;
import static com.allst.springboot.tradeorder.domain.OrderState.ALLOCATE_ROUTE;
import static com.allst.springboot.tradeorder.domain.OrderState.CANCELLED;
import static com.allst.springboot.tradeorder.domain.OrderState.CLOSED;
import static com.allst.springboot.tradeorder.domain.OrderState.COMPLETED;
import static com.allst.springboot.tradeorder.domain.OrderState.CREATED;
import static com.allst.springboot.tradeorder.domain.OrderState.DELIVERED;
import static com.allst.springboot.tradeorder.domain.OrderState.PAYING;
import static com.allst.springboot.tradeorder.domain.OrderState.PAY_FAILED;
import static com.allst.springboot.tradeorder.domain.OrderState.PAY_ROUTE;
import static com.allst.springboot.tradeorder.domain.OrderState.REFUNDED;
import static com.allst.springboot.tradeorder.domain.OrderState.REFUNDING;
import static com.allst.springboot.tradeorder.domain.OrderState.RETURN_APPLIED;
import static com.allst.springboot.tradeorder.domain.OrderState.RETURN_APPROVED;
import static com.allst.springboot.tradeorder.domain.OrderState.RETURN_REJECTED;
import static com.allst.springboot.tradeorder.domain.OrderState.RISK_REJECTED;
import static com.allst.springboot.tradeorder.domain.OrderState.RISK_REVIEW;
import static com.allst.springboot.tradeorder.domain.OrderState.SHIPPED;
import static com.allst.springboot.tradeorder.domain.OrderState.STOCK_SHORT;
import static com.allst.springboot.tradeorder.domain.OrderState.WAIT_ALLOCATE;
import static com.allst.springboot.tradeorder.domain.OrderState.WAIT_SHIP;

@Configuration
@EnableStateMachineFactory(name = "tradeOrderMachineFactory")
public class TradeOrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    private final AmountGuard amountGuard;
    private final PayWindowGuard payWindowGuard;
    private final HighRiskGuard highRiskGuard;
    private final ReserveStockGuard reserveStockGuard;
    private final ShipmentNoGuard shipmentNoGuard;
    private final MarkPaidAction markPaidAction;
    private final ShipAction shipAction;
    private final NotifyWarehouseAction notifyWarehouseAction;
    private final NotifyRefundAction notifyRefundAction;
    private final NotifyCompletedAction notifyCompletedAction;
    private final TradeOrderEffectListener effectListener;

    public TradeOrderStateMachineConfig(AmountGuard amountGuard,
                                        PayWindowGuard payWindowGuard,
                                        HighRiskGuard highRiskGuard,
                                        ReserveStockGuard reserveStockGuard,
                                        ShipmentNoGuard shipmentNoGuard,
                                        MarkPaidAction markPaidAction,
                                        ShipAction shipAction,
                                        NotifyWarehouseAction notifyWarehouseAction,
                                        NotifyRefundAction notifyRefundAction,
                                        NotifyCompletedAction notifyCompletedAction,
                                        TradeOrderEffectListener effectListener) {
        this.amountGuard = amountGuard;
        this.payWindowGuard = payWindowGuard;
        this.highRiskGuard = highRiskGuard;
        this.reserveStockGuard = reserveStockGuard;
        this.shipmentNoGuard = shipmentNoGuard;
        this.markPaidAction = markPaidAction;
        this.shipAction = shipAction;
        this.notifyWarehouseAction = notifyWarehouseAction;
        this.notifyRefundAction = notifyRefundAction;
        this.notifyCompletedAction = notifyCompletedAction;
        this.effectListener = effectListener;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false)
                .listener(effectListener);
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(CREATED)
                .state(PAYING)
                .state(PAY_FAILED)
                .choice(PAY_ROUTE)
                .state(RISK_REVIEW)
                .state(RISK_REJECTED)
                .state(WAIT_ALLOCATE)
                .choice(ALLOCATE_ROUTE)
                .state(STOCK_SHORT)
                .state(WAIT_SHIP)
                .state(SHIPPED)
                .state(DELIVERED)
                .state(AFTER_SALE)
                .state(REFUNDING)
                .end(REFUNDED)
                .end(COMPLETED)
                .end(CANCELLED)
                .end(CLOSED)
                .and()
                .withStates()
                .parent(AFTER_SALE)
                .initial(RETURN_APPLIED)
                .state(RETURN_APPROVED)
                .state(RETURN_REJECTED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal().source(CREATED).target(PAYING).event(SUBMIT_PAY)
                .guard(GuardChain.all(amountGuard, payWindowGuard))
                .and()
                .withExternal().source(CREATED).target(CANCELLED).event(CANCEL)
                .and()
                .withExternal().source(CREATED).target(CLOSED).event(TIMEOUT)
                .and()
                .withExternal().source(PAY_FAILED).target(PAYING).event(SUBMIT_PAY)
                .guard(GuardChain.all(amountGuard, payWindowGuard))
                .and()
                .withExternal().source(PAY_FAILED).target(CANCELLED).event(CANCEL)
                .and()
                .withExternal().source(PAY_FAILED).target(CLOSED).event(TIMEOUT)
                .and()
                .withExternal().source(PAYING).target(PAY_FAILED).event(PAY_FAIL)
                .and()
                .withExternal().source(PAYING).target(CLOSED).event(TIMEOUT)
                .and()
                .withExternal().source(PAYING).target(PAY_ROUTE).event(PAY_SUCCESS).action(markPaidAction)
                .and()
                .withChoice().source(PAY_ROUTE).first(RISK_REVIEW, highRiskGuard).last(WAIT_ALLOCATE)
                .and()
                .withExternal().source(RISK_REVIEW).target(WAIT_ALLOCATE).event(RISK_PASS)
                .and()
                .withExternal().source(RISK_REVIEW).target(RISK_REJECTED).event(RISK_REJECT)
                .and()
                .withExternal().source(RISK_REJECTED).target(REFUNDING).event(START_REFUND).action(notifyRefundAction)
                .and()
                .withExternal().source(WAIT_ALLOCATE).target(ALLOCATE_ROUTE).event(ALLOCATE)
                .and()
                .withChoice().source(ALLOCATE_ROUTE).first(WAIT_SHIP, reserveStockGuard, notifyWarehouseAction).last(STOCK_SHORT)
                .and()
                .withExternal().source(STOCK_SHORT).target(ALLOCATE_ROUTE).event(ALLOCATE)
                .and()
                .withExternal().source(STOCK_SHORT).target(REFUNDING).event(START_REFUND).action(notifyRefundAction)
                .and()
                .withExternal().source(WAIT_ALLOCATE).target(REFUNDING).event(START_REFUND).action(notifyRefundAction)
                .and()
                .withExternal().source(WAIT_SHIP).target(SHIPPED).event(SHIP).guard(shipmentNoGuard).action(shipAction)
                .and()
                .withExternal().source(WAIT_SHIP).target(REFUNDING).event(START_REFUND).action(notifyRefundAction)
                .and()
                .withExternal().source(SHIPPED).target(DELIVERED).event(SIGN)
                .and()
                .withExternal().source(DELIVERED).target(COMPLETED).event(CONFIRM).action(notifyCompletedAction)
                .and()
                .withExternal().source(DELIVERED).target(AFTER_SALE).event(APPLY_RETURN)
                .and()
                .withExternal().source(RETURN_APPLIED).target(RETURN_APPROVED).event(APPROVE_RETURN)
                .and()
                .withExternal().source(RETURN_APPLIED).target(RETURN_REJECTED).event(REJECT_RETURN)
                .and()
                .withExternal().source(RETURN_REJECTED).target(DELIVERED).event(CLOSE_AFTER_SALE)
                .and()
                .withExternal().source(RETURN_APPROVED).target(REFUNDING).event(START_REFUND).action(notifyRefundAction)
                .and()
                .withExternal().source(REFUNDING).target(REFUNDED).event(REFUND_SUCCESS);
    }
}
