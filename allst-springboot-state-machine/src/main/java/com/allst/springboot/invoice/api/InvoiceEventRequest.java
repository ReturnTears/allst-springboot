package com.allst.springboot.invoice.api;

import com.allst.springboot.invoice.domain.InvoiceEvent;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class InvoiceEventRequest {

    @NotNull(message = "不能为空")
    private InvoiceEvent event;

    @NotBlank(message = "不能为空")
    private String operator;

    @NotBlank(message = "不能为空")
    private String eventId;

    /** 教学字段。生产环境角色来自登录态，不从请求体读取。 */
    private String role;
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
