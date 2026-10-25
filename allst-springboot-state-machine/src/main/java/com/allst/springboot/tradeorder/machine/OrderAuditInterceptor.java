package com.allst.springboot.tradeorder.machine;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 在事件真正执行前给消息补一个受理时间。状态是否改变仍由配置决定。
 */
@Component
public class OrderAuditInterceptor extends StateMachineInterceptorAdapter<OrderState, OrderEvent> {

    @Override
    public org.springframework.messaging.Message<OrderEvent> preEvent(
            org.springframework.messaging.Message<OrderEvent> message,
            StateMachine<OrderState, OrderEvent> stateMachine) {
        return org.springframework.messaging.support.MessageBuilder.fromMessage(message)
                .setHeader("machineAcceptedAt", LocalDateTime.now().toString())
                .build();
    }

    @Override
    public void preStateChange(org.springframework.statemachine.state.State<OrderState, OrderEvent> state,
                               org.springframework.messaging.Message<OrderEvent> message,
                               Transition<OrderState, OrderEvent> transition,
                               StateMachine<OrderState, OrderEvent> stateMachine,
                               StateMachine<OrderState, OrderEvent> rootStateMachine) {
        // 留给后续扩展：需要统一拦截每次状态写入时从这里进入。
    }
}
