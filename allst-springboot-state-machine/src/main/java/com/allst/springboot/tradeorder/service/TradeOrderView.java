package com.allst.springboot.tradeorder.service;

import com.allst.springboot.tradeorder.domain.OrderState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TradeOrderView {

    private String orderNo;
    private String buyerName;
    private String skuCode;
    private Integer quantity;
    private BigDecimal amount;
    private Integer riskScore;
    private OrderState status;
    private String statusLabel;
    private String shipmentNo;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private boolean replayed;
    private List<EventOption> allowedEvents;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public OrderState getStatus() {
        return status;
    }

    public void setStatus(OrderState status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getShipmentNo() {
        return shipmentNo;
    }

    public void setShipmentNo(String shipmentNo) {
        this.shipmentNo = shipmentNo;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
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
