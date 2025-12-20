package com.allst.springboot.service.impl;

import com.allst.springboot.entity.SalesRecord;
import com.allst.springboot.entity.SalesSummary;
import com.allst.springboot.service.SalesService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author Hutu
 * @since 2025-10-31 下午 10:02
 */
@Service
public class SalesServiceImpl implements SalesService {
    @Override
    public SalesSummary fetchSummary() {
        List<SalesRecord> records = Arrays.asList(
                new SalesRecord(new BigDecimal("10"), new BigDecimal("20"), null),
                new SalesRecord(new BigDecimal("5"), null, new BigDecimal("30")),
                new SalesRecord(null, new BigDecimal("10"), new BigDecimal("5"))
        );
        return sumAll(records);
    }

    public SalesSummary sumAll(List<SalesRecord> records) {
        return records.stream()
                .reduce(
                        new SalesSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), // 初始值
                        (summary, record) -> summary.merge(new SalesSummary(getVal(record.getAmount1()), getVal(record.getAmount2()), getVal(record.getAmount3()))),
                        SalesSummary::merge // 并行流时的合并函数（即使不用并行也需提供）
                );
    }
}
