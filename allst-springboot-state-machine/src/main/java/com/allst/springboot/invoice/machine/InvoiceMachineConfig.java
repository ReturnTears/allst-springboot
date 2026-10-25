package com.allst.springboot.invoice.machine;

import com.alibaba.cola.statemachine.StateMachine;
import com.allst.springboot.invoice.domain.InvoiceContext;
import com.allst.springboot.invoice.domain.InvoiceEvent;
import com.allst.springboot.invoice.domain.InvoiceState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceMachineConfig {

    @Bean
    public StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> invoiceStateMachine() {
        return InvoiceMachineFactory.getOrBuild();
    }
}
