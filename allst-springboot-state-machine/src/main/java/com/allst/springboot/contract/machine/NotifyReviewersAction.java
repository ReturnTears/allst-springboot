package com.allst.springboot.contract.machine;

import com.allst.springboot.common.async.AfterCommitExecutor;
import com.allst.springboot.contract.domain.ContractEvent;
import com.allst.springboot.contract.domain.ContractState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class NotifyReviewersAction implements Action<ContractState, ContractEvent> {

    private static final Logger log = LoggerFactory.getLogger(NotifyReviewersAction.class);

    private final AfterCommitExecutor afterCommitExecutor;

    public NotifyReviewersAction(AfterCommitExecutor afterCommitExecutor) {
        this.afterCommitExecutor = afterCommitExecutor;
    }

    @Override
    public void execute(StateContext<ContractState, ContractEvent> context) {
        String contractNo = context.getStateMachine().getId();
        afterCommitExecutor.execute("legal-review:" + contractNo,
                () -> log.info("通知法务审核合同 {}", contractNo));
        afterCommitExecutor.execute("finance-review:" + contractNo,
                () -> log.info("通知财务审核合同 {}", contractNo));
    }
}
