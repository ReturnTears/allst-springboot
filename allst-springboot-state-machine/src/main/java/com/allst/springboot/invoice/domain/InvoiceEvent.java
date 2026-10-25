package com.allst.springboot.invoice.domain;

public enum InvoiceEvent {

    SUBMIT("提交"),
    APPROVE("审核通过"),
    REJECT("驳回"),
    RESUBMIT("重新提交"),
    VOID("作废"),
    RED_FLUSH("红冲");

    private final String label;

    InvoiceEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
