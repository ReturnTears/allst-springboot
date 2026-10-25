package com.allst.springboot.tradeorder.machine;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 状态机实例的固定生命周期：取出、停机、按备忘录恢复、发送一条事件、再停机。
 * 实例不跨请求复用，因此也不跨线程复用。
 */
@Component
public class OrderMachineTemplate {

    private final StateMachineFactory<OrderState, OrderEvent> factory;
    private final OrderAuditInterceptor interceptor;

    public OrderMachineTemplate(@Qualifier("tradeOrderMachineFactory")
                                StateMachineFactory<OrderState, OrderEvent> factory,
                                OrderAuditInterceptor interceptor) {
        this.factory = factory;
        this.interceptor = interceptor;
    }

    public FireResult fire(String orderNo, OrderState current, OrderEvent event,
                           Map<String, Object> variables, Map<String, Object> headers) {
        StateMachine<OrderState, OrderEvent> machine = factory.getStateMachine(orderNo);
        try {
            machine.stop();
            machine.getStateMachineAccessor().doWithRegion(access -> access.addStateMachineInterceptor(interceptor));
            machine.getStateMachineAccessor().doWithRegion(access -> access.resetStateMachine(contextOf(orderNo, current)));
            machine.start();
            machine.getExtendedState().getVariables().putAll(variables);
            MessageBuilder<OrderEvent> builder = MessageBuilder.withPayload(event);
            if (headers != null) {
                headers.forEach((key, value) -> {
                    if (value != null) {
                        builder.setHeader(key, value);
                    }
                });
            }
            boolean accepted = machine.sendEvent(builder.build());
            Object reason = machine.getExtendedState().getVariables().get("rejectReason");
            return new FireResult(accepted, OrderStates.leaf(machine), reason == null ? null : String.valueOf(reason));
        } finally {
            machine.stop();
        }
    }

    /**
     * 售后子状态不能只把叶子状态写进根机器。根上下文要停在父状态 AFTER_SALE，子上下文再指向具体售后状态。
     */
    private StateMachineContext<OrderState, OrderEvent> contextOf(String orderNo, OrderState current) {
        if (current == OrderState.RETURN_APPLIED
                || current == OrderState.RETURN_APPROVED
                || current == OrderState.RETURN_REJECTED) {
            StateMachineContext<OrderState, OrderEvent> child =
                    new DefaultStateMachineContext<>(current, null, null, null, null, orderNo);
            return new DefaultStateMachineContext<>(Collections.singletonList(child), OrderState.AFTER_SALE,
                    null, null, null, null, orderNo);
        }
        return new DefaultStateMachineContext<>(current, null, null, null, null, orderNo);
    }

    public static final class FireResult {
        private final boolean accepted;
        private final OrderState state;
        private final String rejectReason;

        public FireResult(boolean accepted, OrderState state, String rejectReason) {
            this.accepted = accepted;
            this.state = state;
            this.rejectReason = rejectReason;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public OrderState getState() {
            return state;
        }

        public String getRejectReason() {
            return rejectReason;
        }
    }
}
