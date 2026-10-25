package com.allst.springboot.invoice.domain;

public class InvoiceEventCommand {

    private InvoiceEvent event;
    private String operator;
    private String role;
    private String eventId;
    private String remark;

    public InvoiceEvent getEvent() {
        return event;
    }

    public void setEvent(InvoiceEvent event) {
        this.event = event;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
