package com.allst.springboot.contract.machine;

import com.allst.springboot.contract.domain.ContractEvent;
import com.allst.springboot.contract.domain.ContractState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import static com.allst.springboot.contract.domain.ContractEvent.CANCEL;
import static com.allst.springboot.contract.domain.ContractEvent.FINANCE_PASS;
import static com.allst.springboot.contract.domain.ContractEvent.FINANCE_REJECT;
import static com.allst.springboot.contract.domain.ContractEvent.LEGAL_PASS;
import static com.allst.springboot.contract.domain.ContractEvent.LEGAL_REJECT;
import static com.allst.springboot.contract.domain.ContractEvent.SIGN;
import static com.allst.springboot.contract.domain.ContractEvent.SUBMIT;
import static com.allst.springboot.contract.domain.ContractState.CANCELLED;
import static com.allst.springboot.contract.domain.ContractState.DRAFT;
import static com.allst.springboot.contract.domain.ContractState.FINANCE_PASSED;
import static com.allst.springboot.contract.domain.ContractState.FINANCE_PENDING;
import static com.allst.springboot.contract.domain.ContractState.LEGAL_PASSED;
import static com.allst.springboot.contract.domain.ContractState.LEGAL_PENDING;
import static com.allst.springboot.contract.domain.ContractState.REJECTED;
import static com.allst.springboot.contract.domain.ContractState.REVIEWING;
import static com.allst.springboot.contract.domain.ContractState.REVIEW_FORK;
import static com.allst.springboot.contract.domain.ContractState.REVIEW_JOIN;
import static com.allst.springboot.contract.domain.ContractState.SIGNED;
import static com.allst.springboot.contract.domain.ContractState.WAIT_SIGN;

@Configuration
@EnableStateMachineFactory(name = "contractMachineFactory")
public class ContractStateMachineConfig extends StateMachineConfigurerAdapter<ContractState, ContractEvent> {

    public static final String LEGAL_REGION = "LEGAL";
    public static final String FINANCE_REGION = "FINANCE";

    private final NotifyReviewersAction notifyReviewersAction;

    public ContractStateMachineConfig(NotifyReviewersAction notifyReviewersAction) {
        this.notifyReviewersAction = notifyReviewersAction;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<ContractState, ContractEvent> config) throws Exception {
        config.withConfiguration().autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<ContractState, ContractEvent> states) throws Exception {
        states.withStates()
                .initial(DRAFT)
                .fork(REVIEW_FORK)
                .state(REVIEWING)
                .join(REVIEW_JOIN)
                .state(WAIT_SIGN)
                .end(SIGNED)
                .end(REJECTED)
                .end(CANCELLED)
                .and()
                .withStates()
                .parent(REVIEWING)
                .region(LEGAL_REGION)
                .initial(LEGAL_PENDING)
                .end(LEGAL_PASSED)
                .and()
                .withStates()
                .parent(REVIEWING)
                .region(FINANCE_REGION)
                .initial(FINANCE_PENDING)
                .end(FINANCE_PASSED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ContractState, ContractEvent> transitions) throws Exception {
        transitions
                .withExternal().source(DRAFT).target(REVIEW_FORK).event(SUBMIT).action(notifyReviewersAction)
                .and()
                .withFork().source(REVIEW_FORK).target(LEGAL_PENDING).target(FINANCE_PENDING)
                .and()
                .withExternal().source(LEGAL_PENDING).target(LEGAL_PASSED).event(LEGAL_PASS)
                .and()
                .withExternal().source(LEGAL_PENDING).target(REJECTED).event(LEGAL_REJECT)
                .and()
                .withExternal().source(FINANCE_PENDING).target(FINANCE_PASSED).event(FINANCE_PASS)
                .and()
                .withExternal().source(FINANCE_PENDING).target(REJECTED).event(FINANCE_REJECT)
                .and()
                .withJoin().source(LEGAL_PASSED).source(FINANCE_PASSED).target(REVIEW_JOIN)
                .and()
                .withExternal().source(REVIEW_JOIN).target(WAIT_SIGN)
                .and()
                .withExternal().source(WAIT_SIGN).target(SIGNED).event(SIGN)
                .and()
                .withExternal().source(DRAFT).target(CANCELLED).event(CANCEL);
    }
}
