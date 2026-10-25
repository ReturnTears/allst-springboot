package com.allst.springboot.tradeorder.api;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateTradeOrderRequest {

    @NotBlank(message = "不能为空")
    private String buyerName;

    @NotBlank(message = "不能为空")
    private String skuCode;

    @NotNull(message = "不能为空")
    @Min(value = 1, message = "至少为 1")
    private Integer quantity;

    @NotNull(message = "不能为空")
    @DecimalMin(value = "0.01", message = "必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "不能为空")
    @Min(value = 0, message = "不能小于 0")
    @Max(value = 100, message = "不能大于 100")
    private Integer riskScore;

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
}
