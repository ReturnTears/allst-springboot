package com.allst.springboot.contract.service;

import com.allst.springboot.audit.TransitionLog;
import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.contract.domain.ContractEvent;
import com.allst.springboot.contract.domain.ContractEventCatalog;
import com.allst.springboot.contract.domain.ContractEventCommand;
import com.allst.springboot.contract.domain.ContractState;
import com.allst.springboot.contract.domain.SalesContract;
import com.allst.springboot.contract.machine.ContractMachineTemplate;
import com.allst.springboot.contract.repo.SalesContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ContractFlowService {

    public static final String MACHINE_TYPE = "CONTRACT";

    private final SalesContractRepository contractRepository;
    private final TransitionLogRepository logRepository;
    private final ContractMachineTemplate machineTemplate;
    private final ContractAssembler assembler;

    public ContractFlowService(SalesContractRepository contractRepository,
                               TransitionLogRepository logRepository,
                               ContractMachineTemplate machineTemplate,
                               ContractAssembler assembler) {
        this.contractRepository = contractRepository;
        this.logRepository = logRepository;
        this.machineTemplate = machineTemplate;
        this.assembler = assembler;
    }

    @Transactional
    public ContractView create(String title, BigDecimal amount) {
        SalesContract contract = new SalesContract();
        contract.setContractNo("C" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 10000));
        contract.setTitle(title);
        contract.setAmount(amount);
        contract.setPhase(ContractState.DRAFT);
        return assembler.toView(contractRepository.save(contract), false);
    }

    @Transactional
    public ContractView fire(String contractNo, ContractEventCommand command) {
        SalesContract contract = contractRepository.findByContractNo(contractNo)
                .orElseThrow(() -> new BizException(404, "合同不存在"));
        if (logRepository.existsByMachineTypeAndMachineIdAndEventId(MACHINE_TYPE, contractNo, command.getEventId())) {
            return assembler.toView(contract, true);
        }
        ContractState before = contract.getPhase();
        Map<String, Object> headers = new HashMap<>();
        headers.put("operator", command.getOperator());
        headers.put("eventId", command.getEventId());
        ContractMachineTemplate.FireResult result = machineTemplate.fire(contract, command.getEvent(), headers);
        if (!result.isAccepted()) {
            if (ContractEventCatalog.allowed(before, contract.getLegalStatus(), contract.getFinanceStatus())
                    .contains(command.getEvent())) {
                throw new BizException("事件「" + command.getEvent().getLabel() + "」被守卫拒绝");
            }
            throw new BizException("合同阶段「" + before.getLabel() + "」不能接受事件「" + command.getEvent().getLabel() + "」");
        }
        apply(contract, command.getEvent(), result.getActiveStates());
        if (contract.getPhase() == before
                && sameTrack(contract, command.getEvent())) {
            throw new BizException("合同状态没有变化");
        }
        saveLog(contract, before, command);
        return assembler.toView(contract, false);
    }

    private void apply(SalesContract contract, ContractEvent event, Collection<ContractState> ids) {
        ContractState legal = track(ids, ContractState.LEGAL_PENDING, ContractState.LEGAL_PASSED, ContractState.LEGAL_REJECTED);
        ContractState finance = track(ids, ContractState.FINANCE_PENDING, ContractState.FINANCE_PASSED, ContractState.FINANCE_REJECTED);
        if (event == ContractEvent.LEGAL_PASS) {
            legal = ContractState.LEGAL_PASSED;
        } else if (event == ContractEvent.LEGAL_REJECT) {
            legal = ContractState.LEGAL_REJECTED;
        }
        if (event == ContractEvent.FINANCE_PASS) {
            finance = ContractState.FINANCE_PASSED;
        } else if (event == ContractEvent.FINANCE_REJECT) {
            finance = ContractState.FINANCE_REJECTED;
        }
        if (legal != null) {
            contract.setLegalStatus(legal);
        }
        if (finance != null) {
            contract.setFinanceStatus(finance);
        }
        contract.setPhase(phaseOf(ids));
    }

    private boolean sameTrack(SalesContract contract, ContractEvent event) {
        return event != ContractEvent.LEGAL_PASS
                && event != ContractEvent.FINANCE_PASS
                && event != ContractEvent.LEGAL_REJECT
                && event != ContractEvent.FINANCE_REJECT;
    }

    private ContractState phaseOf(Collection<ContractState> ids) {
        if (ids.contains(ContractState.SIGNED)) {
            return ContractState.SIGNED;
        }
        if (ids.contains(ContractState.REJECTED)) {
            return ContractState.REJECTED;
        }
        if (ids.contains(ContractState.CANCELLED)) {
            return ContractState.CANCELLED;
        }
        if (ids.contains(ContractState.WAIT_SIGN)) {
            return ContractState.WAIT_SIGN;
        }
        if (ids.contains(ContractState.REVIEWING)
                || ids.contains(ContractState.LEGAL_PENDING)
                || ids.contains(ContractState.LEGAL_PASSED)
                || ids.contains(ContractState.FINANCE_PENDING)
                || ids.contains(ContractState.FINANCE_PASSED)) {
            return ContractState.REVIEWING;
        }
        if (ids.contains(ContractState.DRAFT)) {
            return ContractState.DRAFT;
        }
        throw new BizException("无法识别合同状态: " + ids);
    }

    private ContractState track(Collection<ContractState> ids, ContractState pending, ContractState passed, ContractState rejected) {
        if (ids.contains(rejected)) {
            return rejected;
        }
        if (ids.contains(passed)) {
            return passed;
        }
        if (ids.contains(pending)) {
            return pending;
        }
        return null;
    }

    private void saveLog(SalesContract contract, ContractState before, ContractEventCommand command) {
        TransitionLog log = new TransitionLog();
        log.setMachineType(MACHINE_TYPE);
        log.setMachineId(contract.getContractNo());
        log.setFromState(before.name());
        log.setToState(contract.getPhase().name());
        log.setEvent(command.getEvent().name());
        log.setOperator(command.getOperator());
        log.setEventId(command.getEventId());
        String remark = command.getRemark();
        if (contract.getPhase() == ContractState.REVIEWING) {
            remark = "法务=" + contract.getLegalStatus() + ", 财务=" + contract.getFinanceStatus();
        }
        log.setRemark(remark);
        logRepository.save(log);
    }
}
