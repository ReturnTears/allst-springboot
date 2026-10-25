package com.allst.springboot.contract.domain;

import java.util.ArrayList;
import java.util.List;

public final class ContractEventCatalog {

    private ContractEventCatalog() {
    }

    public static List<ContractEvent> allowed(ContractState phase, ContractState legalStatus, ContractState financeStatus) {
        if (phase == ContractState.DRAFT) {
            return List.of(ContractEvent.SUBMIT, ContractEvent.CANCEL);
        }
        if (phase == ContractState.REVIEWING) {
            List<ContractEvent> events = new ArrayList<>();
            if (legalStatus == ContractState.LEGAL_PENDING) {
                events.add(ContractEvent.LEGAL_PASS);
                events.add(ContractEvent.LEGAL_REJECT);
            }
            if (financeStatus == ContractState.FINANCE_PENDING) {
                events.add(ContractEvent.FINANCE_PASS);
                events.add(ContractEvent.FINANCE_REJECT);
            }
            return events;
        }
        if (phase == ContractState.WAIT_SIGN) {
            return List.of(ContractEvent.SIGN);
        }
        return List.of();
    }
}
