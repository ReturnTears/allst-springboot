package com.allst.springboot.contract.machine;

import com.allst.springboot.contract.domain.ContractEvent;
import com.allst.springboot.contract.domain.ContractState;
import com.allst.springboot.contract.domain.SalesContract;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class ContractMachineTemplate {

    private final StateMachineFactory<ContractState, ContractEvent> factory;

    public ContractMachineTemplate(@Qualifier("contractMachineFactory")
                                   StateMachineFactory<ContractState, ContractEvent> factory) {
        this.factory = factory;
    }

    public FireResult fire(SalesContract contract, ContractEvent event, Map<String, Object> headers) {
        String contractNo = contract.getContractNo();
        StateMachine<ContractState, ContractEvent> machine = factory.getStateMachine(contractNo);
        try {
            machine.stop();
            machine.getStateMachineAccessor().doWithRegion(access ->
                    access.resetStateMachine(contextOf(contract)));
            machine.start();
            MessageBuilder<ContractEvent> builder = MessageBuilder.withPayload(event);
            if (headers != null) {
                headers.forEach((key, value) -> {
                    if (value != null) {
                        builder.setHeader(key, value);
                    }
                });
            }
            boolean accepted = machine.sendEvent(builder.build());
            Collection<ContractState> ids = machine.getState() == null
                    ? List.of()
                    : machine.getState().getIds();
            return new FireResult(accepted, ids);
        } finally {
            machine.stop();
        }
    }

    private DefaultStateMachineContext<ContractState, ContractEvent> contextOf(SalesContract contract) {
        if (contract.getPhase() == ContractState.REVIEWING
                && contract.getLegalStatus() != null
                && contract.getFinanceStatus() != null) {
            List<org.springframework.statemachine.StateMachineContext<ContractState, ContractEvent>> children = new ArrayList<>();
            children.add(new DefaultStateMachineContext<>(contract.getLegalStatus(), null, null, null, null,
                    ContractStateMachineConfig.LEGAL_REGION));
            children.add(new DefaultStateMachineContext<>(contract.getFinanceStatus(), null, null, null, null,
                    ContractStateMachineConfig.FINANCE_REGION));
            return new DefaultStateMachineContext<>(children, ContractState.REVIEWING, null, null, null, null,
                    contract.getContractNo());
        }
        return new DefaultStateMachineContext<>(contract.getPhase(), null, null, null, null, contract.getContractNo());
    }

    public static final class FireResult {
        private final boolean accepted;
        private final Collection<ContractState> activeStates;

        public FireResult(boolean accepted, Collection<ContractState> activeStates) {
            this.accepted = accepted;
            this.activeStates = activeStates;
        }

        public boolean isAccepted() {
            return accepted;
        }

        public Collection<ContractState> getActiveStates() {
            return activeStates;
        }
    }
}
