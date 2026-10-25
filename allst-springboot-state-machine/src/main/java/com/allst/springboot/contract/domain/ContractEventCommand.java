package com.allst.springboot.contract.domain;

public class ContractEventCommand {

    private ContractEvent event;
    private String operator;
    private String eventId;
    private String remark;

    public ContractEvent getEvent() {
        return event;
    }

    public void setEvent(ContractEvent event) {
        this.event = event;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
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
