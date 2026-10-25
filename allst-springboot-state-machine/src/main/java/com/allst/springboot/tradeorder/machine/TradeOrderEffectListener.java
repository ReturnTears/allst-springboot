package com.allst.springboot.tradeorder.machine;

import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * 观察者：只记录已经发生的转移。外部通知放在转移动作里，避免监听器在状态恢复时重放。
 */
@Component
public class TradeOrderEffectListener extends StateMachineListenerAdapter<OrderState, OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(TradeOrderEffectListener.class);

    @Override
    public void stateContext(StateContext<OrderState, OrderEvent> context) {
        if (context.getStage() != StateContext.Stage.TRANSITION_END
                || context.getSource() == null
                || context.getTarget() == null) {
            return;
        }
        log.info("订单 {} 状态 {} -> {}，事件 {}",
                context.getStateMachine().getId(),
                context.getSource().getId(),
                context.getTarget().getId(),
                context.getEvent());
    }
}
