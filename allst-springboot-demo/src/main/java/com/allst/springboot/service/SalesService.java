package com.allst.springboot.service;

import com.allst.springboot.entity.SalesSummary;

import java.math.BigDecimal;

/**
 * @author Hutu
 * @since 2025-10-31 下午 09:29
 */
public interface SalesService {
    SalesSummary fetchSummary();
    default BigDecimal getVal(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
