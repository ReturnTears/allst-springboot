package com.allst.springboot.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Hutu
 * @since 2025-10-31 下午 09:25
 */
@Data
public class SalesSummary {
    public final BigDecimal total1;
    public final BigDecimal total2;
    public final BigDecimal total3;

    public SalesSummary(BigDecimal t1, BigDecimal t2, BigDecimal t3) {
        this.total1 = t1 != null ? t1 : BigDecimal.ZERO;
        this.total2 = t2 != null ? t2 : BigDecimal.ZERO;
        this.total3 = t3 != null ? t3 : BigDecimal.ZERO;
    }

    // 合并两个 Summary（用于 reduce 的 combiner）
    public SalesSummary merge(SalesSummary other) {
        return new SalesSummary(
                this.total1.add(other.total1),
                this.total2.add(other.total2),
                this.total3.add(other.total3)
        );
    }
}
