package com.allst.springboot.invoice.machine;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.cola.statemachine.StateMachineFactory;
import com.alibaba.cola.statemachine.builder.StateMachineBuilder;
import com.alibaba.cola.statemachine.builder.StateMachineBuilderFactory;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.invoice.domain.InvoiceContext;
import com.allst.springboot.invoice.domain.InvoiceEvent;
import com.allst.springboot.invoice.domain.InvoiceState;

import java.math.BigDecimal;

/**
 * COLA 状态机是无状态定义：每次 fireEvent 都把当前状态当参数传进去，定义本身可以单例、多线程共享。
 */
public final class InvoiceMachineFactory {

    public static final String MACHINE_ID = "invoiceApply";
    public static final BigDecimal FINANCE_THRESHOLD = new BigDecimal("10000");

    private InvoiceMachineFactory() {
    }

    public static synchronized StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> getOrBuild() {
        try {
            return StateMachineFactory.get(MACHINE_ID);
        } catch (RuntimeException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("no stateMachine")) {
                throw exception;
            }
            return build();
        }
    }

    private static StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> build() {
        StateMachineBuilder<InvoiceState, InvoiceEvent, InvoiceContext> builder = StateMachineBuilderFactory.create();
        builder.externalTransition()
                .from(InvoiceState.DRAFT)
                .to(InvoiceState.PENDING_REVIEW)
                .on(InvoiceEvent.SUBMIT)
                .when(InvoiceMachineFactory::amountPositive)
                .perform((from, to, event, context) -> { });
        builder.externalTransition()
                .from(InvoiceState.DRAFT)
                .to(InvoiceState.VOIDED)
                .on(InvoiceEvent.VOID)
                .perform((from, to, event, context) -> { });
        builder.externalTransition()
                .from(InvoiceState.PENDING_REVIEW)
                .to(InvoiceState.ISSUED)
                .on(InvoiceEvent.APPROVE)
                .perform(InvoiceMachineFactory::approve);
        builder.externalTransition()
                .from(InvoiceState.PENDING_REVIEW)
                .to(InvoiceState.REJECTED)
                .on(InvoiceEvent.REJECT)
                .perform((from, to, event, context) -> { });
        builder.externalTransition()
                .from(InvoiceState.REJECTED)
                .to(InvoiceState.PENDING_REVIEW)
                .on(InvoiceEvent.RESUBMIT)
                .when(InvoiceMachineFactory::amountPositive)
                .perform((from, to, event, context) -> { });
        builder.externalTransition()
                .from(InvoiceState.REJECTED)
                .to(InvoiceState.VOIDED)
                .on(InvoiceEvent.VOID)
                .perform((from, to, event, context) -> { });
        builder.externalTransition()
                .from(InvoiceState.ISSUED)
                .to(InvoiceState.VOIDED)
                .on(InvoiceEvent.RED_FLUSH)
                .perform((from, to, event, context) -> { });
        builder.setFailCallback((source, event, context) -> {
            throw new BizException("发票状态「" + source.getLabel() + "」不能接受事件「" + event.getLabel() + "」");
        });
        return builder.build(MACHINE_ID);
    }

    private static boolean amountPositive(InvoiceContext context) {
        return context.getAmount() != null && context.getAmount().signum() > 0;
    }

    private static void approve(InvoiceState from, InvoiceState to, InvoiceEvent event, InvoiceContext context) {
        if (context.getAmount().compareTo(FINANCE_THRESHOLD) >= 0 && !"FINANCE".equals(context.getRole())) {
            throw new BizException("金额达到 " + FINANCE_THRESHOLD.toPlainString() + " 的发票需要 FINANCE 角色审核");
        }
    }
}
