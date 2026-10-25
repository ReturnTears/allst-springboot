package com.allst.springboot.invoice.service;

import com.alibaba.cola.statemachine.StateMachine;
import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.lock.KeyStripedLock;
import com.allst.springboot.invoice.domain.InvoiceContext;
import com.allst.springboot.invoice.domain.InvoiceEvent;
import com.allst.springboot.invoice.domain.InvoiceEventCommand;
import com.allst.springboot.invoice.domain.InvoiceState;
import com.allst.springboot.invoice.repo.InvoiceApplyRepository;
import com.allst.springboot.tradeorder.service.TradeOrderAssembler;
import com.allst.springboot.tradeorder.service.TransitionLogView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceAppService {

    private final KeyStripedLock locks;
    private final InvoiceFlowService flowService;
    private final InvoiceApplyRepository invoiceRepository;
    private final TransitionLogRepository logRepository;
    private final TradeOrderAssembler logAssembler;
    private final InvoiceAssembler invoiceAssembler;
    private final StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> machine;

    public InvoiceAppService(KeyStripedLock locks,
                             InvoiceFlowService flowService,
                             InvoiceApplyRepository invoiceRepository,
                             TransitionLogRepository logRepository,
                             TradeOrderAssembler logAssembler,
                             InvoiceAssembler invoiceAssembler,
                             StateMachine<InvoiceState, InvoiceEvent, InvoiceContext> machine) {
        this.locks = locks;
        this.flowService = flowService;
        this.invoiceRepository = invoiceRepository;
        this.logRepository = logRepository;
        this.logAssembler = logAssembler;
        this.invoiceAssembler = invoiceAssembler;
        this.machine = machine;
    }

    public InvoiceView create(String title, BigDecimal amount) {
        return flowService.create(title, amount);
    }

    public InvoiceView fire(String invoiceNo, InvoiceEventCommand command) {
        return locks.call("invoice:" + invoiceNo, () -> flowService.fire(invoiceNo, command));
    }

    @Transactional(readOnly = true)
    public InvoiceView get(String invoiceNo) {
        com.allst.springboot.invoice.domain.InvoiceApply invoice = invoiceRepository.findByInvoiceNo(invoiceNo)
                .orElseThrow(() -> new BizException(404, "发票不存在"));
        return invoiceAssembler.toView(invoice, false);
    }

    @Transactional(readOnly = true)
    public List<TransitionLogView> logs(String invoiceNo) {
        if (!invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            throw new BizException(404, "发票不存在");
        }
        return logRepository.findByMachineTypeAndMachineIdOrderByIdAsc(InvoiceFlowService.MACHINE_TYPE, invoiceNo)
                .stream()
                .map(logAssembler::toLogView)
                .collect(Collectors.toList());
    }

    public String plantUml() {
        return machine.generatePlantUML();
    }
}
