package com.allst.springboot.contract.domain;

public enum ContractState {

    DRAFT("草稿"),
    REVIEW_FORK("审核分叉", true),
    REVIEWING("并行审核", true),
    LEGAL_PENDING("法务待审"),
    LEGAL_PASSED("法务通过"),
    LEGAL_REJECTED("法务拒绝"),
    FINANCE_PENDING("财务待审"),
    FINANCE_PASSED("财务通过"),
    FINANCE_REJECTED("财务拒绝"),
    REVIEW_JOIN("审核汇合", true),
    WAIT_SIGN("待签署"),
    SIGNED("已签署"),
    REJECTED("已驳回"),
    CANCELLED("已取消");

    private final String label;
    private final boolean structural;

    ContractState(String label) {
        this(label, false);
    }

    ContractState(String label, boolean structural) {
        this.label = label;
        this.structural = structural;
    }

    public String getLabel() {
        return label;
    }

    public boolean isStructural() {
        return structural;
    }
}
