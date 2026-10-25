package com.allst.springboot.invoice.service;

import com.alibaba.cola.statemachine.StateMachine;
import com.allst.springboot.audit.TransitionLog;
import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.invoice.domain.InvoiceApply;
import com.allst.springboot.invoice.domain.InvoiceContext;
import com.allst.springboot.invoice.domain.InvoiceEvent;
import com.allst.springboot.invoice.domain.InvoiceEventCommand;
import com.allst.springboot.invoice.domain.InvoiceState;
import com.allst.springboot.invoice.repo.InvoiceApplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InvoiceFlowService {

    public static final String MACHINE_TYPE = "INVOICE";

    private final InvoiceApplyRepository invoiceRepository;
    private final TransitionLogRepository logRepository;
    private final StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> machine;
    private final InvoiceAssembler assembler;

    public InvoiceFlowService(InvoiceApplyRepository invoiceRepository,
                              TransitionLogRepository logRepository,
                              StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> machine,
                              InvoiceAssembler assembler) {
        this.invoiceRepository = invoiceRepository;
        this.logRepository = logRepository;
        this.machine = machine;
        this.assembler = assembler;
    }

    @Transactional
    public InvoiceView create(String title, BigDecimal amount) {
        InvoiceApply invoice = new InvoiceApply();
        invoice.setInvoiceNo("I" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 10000));
        invoice.setTitle(title);
        invoice.setAmount(amount);
        invoice.setStatus(InvoiceState.DRAFT);
        return assembler.toView(invoiceRepository.save(invoice), false);
    }

    @Transactional
    public InvoiceView fire(String invoiceNo, InvoiceEventCommand command) {
        InvoiceApply invoice = invoiceRepository.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new BizException(404, "发票不存在"));
        if (logRepository.existsByMachineTypeAndMachineIdAndEventId(MACHINE_TYPE, invoiceNo, command.getEventId())) {
            return assembler.toView(invoice, true);
        }
        InvoiceState before = invoice.getStatus();
        InvoiceContext context = new InvoiceContext();
        context.setInvoiceNo(invoiceNo);
        context.setAmount(invoice.getAmount());
        context.setOperator(command.getOperator());
        context.setRole(command.getRole());
        context.setRemark(command.getRemark());
        InvoiceState after = machine.fireEvent(before, command.getEvent(), context);
        if (after == before) {
            throw new BizException("发票状态「" + before.getLabel() + "」不能接受事件「" + command.getEvent().getLabel() + "」");
        }
        invoice.setStatus(after);
        if (command.getEvent() == InvoiceEvent.REJECT) {
            invoice.setRejectReason(command.getRemark());
        }
        if (command.getEvent() == InvoiceEvent.RESUBMIT) {
            invoice.setRejectReason(null);
        }
        TransitionLog log = new TransitionLog();
        log.setMachineType(MACHINE_TYPE);
        log.setMachineId(invoiceNo);
        log.setFromState(before.name());
        log.setToState(after.name());
        log.setEvent(command.getEvent().name());
        log.setOperator(command.getOperator());
        log.setEventId(command.getEventId());
        log.setRemark(command.getRemark());
        logRepository.save(log);
        return assembler.toView(invoice, false);
    }
}
