package com.allst.springboot.invoice.service;

import com.allst.springboot.invoice.domain.InvoiceState;
import com.allst.springboot.tradeorder.service.EventOption;

import java.math.BigDecimal;
import java.util.List;

public class InvoiceView {

    private String invoiceNo;
    private String title;
    private BigDecimal amount;
    private InvoiceState status;
    private String statusLabel;
    private String rejectReason;
    private boolean replayed;
    private List<EventOption> allowedEvents;

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
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

    public InvoiceState getStatus() {
        return status;
    }

    public void setStatus(InvoiceState status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
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
