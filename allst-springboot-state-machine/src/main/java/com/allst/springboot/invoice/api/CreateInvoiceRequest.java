package com.allst.springboot.invoice.api;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateInvoiceRequest {

    @NotBlank(message = "不能为空")
    private String title;

    @NotNull(message = "不能为空")
    @DecimalMin(value = "0.01", message = "必须大于 0")
    private BigDecimal amount;

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
}
