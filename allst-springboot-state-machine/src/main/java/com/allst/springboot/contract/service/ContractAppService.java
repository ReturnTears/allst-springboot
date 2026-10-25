package com.allst.springboot.contract.service;

import com.allst.springboot.audit.TransitionLogRepository;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.lock.KeyStripedLock;
import com.allst.springboot.contract.domain.ContractEventCommand;
import com.allst.springboot.contract.domain.SalesContract;
import com.allst.springboot.contract.repo.SalesContractRepository;
import com.allst.springboot.tradeorder.service.TradeOrderAssembler;
import com.allst.springboot.tradeorder.service.TransitionLogView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractAppService {

    private final KeyStripedLock locks;
    private final ContractFlowService flowService;
    private final SalesContractRepository contractRepository;
    private final TransitionLogRepository logRepository;
    private final ContractAssembler assembler;
    private final TradeOrderAssembler logAssembler;

    public ContractAppService(KeyStripedLock locks,
                              ContractFlowService flowService,
                              SalesContractRepository contractRepository,
                              TransitionLogRepository logRepository,
                              ContractAssembler assembler,
                              TradeOrderAssembler logAssembler) {
        this.locks = locks;
        this.flowService = flowService;
        this.contractRepository = contractRepository;
        this.logRepository = logRepository;
        this.assembler = assembler;
        this.logAssembler = logAssembler;
    }

    public ContractView create(String title, BigDecimal amount) {
        return flowService.create(title, amount);
    }

    public ContractView fire(String contractNo, ContractEventCommand command) {
        return locks.call("contract:" + contractNo, () -> flowService.fire(contractNo, command));
    }

    @Transactional(readOnly = true)
    public ContractView get(String contractNo) {
        SalesContract contract = contractRepository.findByContractNo(contractNo)
                .orElseThrow(() -> new BizException(404, "合同不存在"));
        return assembler.toView(contract, false);
    }

    @Transactional(readOnly = true)
    public List<TransitionLogView> logs(String contractNo) {
        if (!contractRepository.existsByContractNo(contractNo)) {
            throw new BizException(404, "合同不存在");
        }
        return logRepository.findByMachineTypeAndMachineIdOrderByIdAsc(ContractFlowService.MACHINE_TYPE, contractNo)
                .stream()
                .map(logAssembler::toLogView)
                .collect(Collectors.toList());
    }
}
