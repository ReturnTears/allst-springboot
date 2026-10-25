package com.allst.springboot.tradeorder.api;

import com.allst.springboot.tradeorder.domain.OrderEvent;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class FireEventRequest {

    @NotNull(message = "不能为空")
    private OrderEvent event;

    @NotBlank(message = "不能为空")
    private String operator;

    @NotBlank(message = "不能为空")
    private String eventId;

    private String remark;
    private String shipmentNo;

    public OrderEvent getEvent() {
        return event;
    }

    public void setEvent(OrderEvent event) {
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

    public String getShipmentNo() {
        return shipmentNo;
    }

    public void setShipmentNo(String shipmentNo) {
        this.shipmentNo = shipmentNo;
    }
}
