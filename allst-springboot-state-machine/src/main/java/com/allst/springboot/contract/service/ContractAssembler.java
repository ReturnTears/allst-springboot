package com.allst.springboot.contract.service;

import com.allst.springboot.contract.domain.ContractEventCatalog;
import com.allst.springboot.contract.domain.ContractState;
import com.allst.springboot.contract.domain.SalesContract;
import com.allst.springboot.tradeorder.service.EventOption;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ContractAssembler {

    public ContractView toView(SalesContract contract, boolean replayed) {
        ContractView view = new ContractView();
        view.setContractNo(contract.getContractNo());
        view.setTitle(contract.getTitle());
        view.setAmount(contract.getAmount());
        view.setPhase(contract.getPhase());
        view.setPhaseLabel(contract.getPhase().getLabel());
        view.setLegalStatus(contract.getLegalStatus());
        view.setLegalStatusLabel(label(contract.getLegalStatus()));
        view.setFinanceStatus(contract.getFinanceStatus());
        view.setFinanceStatusLabel(label(contract.getFinanceStatus()));
        view.setReplayed(replayed);
        view.setAllowedEvents(ContractEventCatalog.allowed(
                        contract.getPhase(), contract.getLegalStatus(), contract.getFinanceStatus())
                .stream()
                .map(event -> new EventOption(event.name(), event.getLabel()))
                .collect(Collectors.toList()));
        return view;
    }

    private String label(ContractState state) {
        return state == null ? null : state.getLabel();
    }
}
