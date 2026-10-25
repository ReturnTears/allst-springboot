package com.allst.springboot.invoice.domain;

public enum InvoiceState {

    DRAFT("草稿"),
    PENDING_REVIEW("待审核"),
    ISSUED("已开具"),
    REJECTED("已驳回"),
    VOIDED("已作废");

    private final String label;

    InvoiceState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
