package com.allst.springboot.contract.api;

import com.allst.springboot.contract.domain.ContractEvent;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ContractEventRequest {

    @NotNull(message = "不能为空")
    private ContractEvent event;

    @NotBlank(message = "不能为空")
    private String operator;

    @NotBlank(message = "不能为空")
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
