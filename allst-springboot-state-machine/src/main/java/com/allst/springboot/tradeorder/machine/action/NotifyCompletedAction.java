package com.allst.springboot.tradeorder.machine.action;

import com.allst.springboot.common.async.AfterCommitExecutor;
import com.allst.springboot.tradeorder.domain.OrderEvent;
import com.allst.springboot.tradeorder.domain.OrderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class NotifyCompletedAction implements Action<OrderState, OrderEvent> {

    private static final Logger log = LoggerFactory.getLogger(NotifyCompletedAction.class);

    private final AfterCommitExecutor afterCommitExecutor;

    public NotifyCompletedAction(AfterCommitExecutor afterCommitExecutor) {
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @Override
    public void execute(StateContext<OrderState, OrderEvent> context) {
        String orderNo = context.getStateMachine().getId();
        afterCommitExecutor.execute("completed:" + orderNo,
                () -> log.info("通知会员系统累计积分，订单 {}", orderNo));
    }
}
