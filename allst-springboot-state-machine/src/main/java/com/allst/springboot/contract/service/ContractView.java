package com.allst.springboot.contract.service;

import com.allst.springboot.contract.domain.ContractState;
import com.allst.springboot.tradeorder.service.EventOption;

import java.math.BigDecimal;
import java.util.List;

public class ContractView {

    private String contractNo;
    private String title;
    private BigDecimal amount;
    private ContractState phase;
    private String phaseLabel;
    private ContractState legalStatus;
    private String legalStatusLabel;
    private ContractState financeStatus;
    private String financeStatusLabel;
    private boolean replayed;
    private List<EventOption> allowedEvents;

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ContractState getPhase() {
        return phase;
    }

    public void setPhase(ContractState phase) {
        this.phase = phase;
    }

    public String getPhaseLabel() {
        return phaseLabel;
    }

    public void setPhaseLabel(String phaseLabel) {
        this.phaseLabel = phaseLabel;
    }

    public ContractState getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(ContractState legalStatus) {
        this.legalStatus = legalStatus;
    }

    public String getLegalStatusLabel() {
        return legalStatusLabel;
    }

    public void setLegalStatusLabel(String legalStatusLabel) {
        this.legalStatusLabel = legalStatusLabel;
    }

    public ContractState getFinanceStatus() {
        return financeStatus;
    }

    public void setFinanceStatus(ContractState financeStatus) {
        this.financeStatus = financeStatus;
    }

    public String getFinanceStatusLabel() {
        return financeStatusLabel;
    }

    public void setFinanceStatusLabel(String financeStatusLabel) {
        this.financeStatusLabel = financeStatusLabel;
    }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }

    public List<EventOption> getAllowedEvents() {
        return allowedEvents;
    }

    public void setAllowedEvents(List<EventOption> allowedEvents) {
        this.allowedEvents = allowedEvents;
    }
}
