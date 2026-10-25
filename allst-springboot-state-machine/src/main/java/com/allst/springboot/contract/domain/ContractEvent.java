package com.allst.springboot.contract.domain;

public enum ContractEvent {

    SUBMIT("提交审核"),
    LEGAL_PASS("法务通过"),
    LEGAL_REJECT("法务驳回"),
    FINANCE_PASS("财务通过"),
    FINANCE_REJECT("财务驳回"),
    SIGN("签署"),
    CANCEL("取消");

    private final String label;

    ContractEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
