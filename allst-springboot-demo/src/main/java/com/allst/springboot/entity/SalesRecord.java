package com.allst.springboot.entity;

import java.math.BigDecimal;

/**
 * @author Hutu
 * @since 2025-10-31 下午 09:25
 */
public class SalesRecord {
    private BigDecimal amount1;
    private BigDecimal amount2;
    private BigDecimal amount3;

    public SalesRecord() {
    }

    public SalesRecord(BigDecimal amount1, BigDecimal amount2, BigDecimal amount3) {
        this.amount1 = amount1;
        this.amount2 = amount2;
        this.amount3 = amount3;
    }

    public BigDecimal getAmount1() {
        return amount1;
    }

    public void setAmount1(BigDecimal amount1) {
        this.amount1 = amount1;
    }

    public BigDecimal getAmount2() {
        return amount2;
    }

    public void setAmount2(BigDecimal amount2) {
        this.amount2 = amount2;
    }

    public BigDecimal getAmount3() {
        return amount3;
    }

    public void setAmount3(BigDecimal amount3) {
        this.amount3 = amount3;
    }
}
