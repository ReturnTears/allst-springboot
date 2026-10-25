package com.allst.springboot.tradeorder.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class ParallelPrecheckOverlapTest {

    @Test
    void riskAndStockQueriesOverlap() throws Exception {
        AtomicLong riskStart = new AtomicLong();
        AtomicLong riskEnd = new AtomicLong();
        AtomicLong stockStart = new AtomicLong();
        AtomicLong stockEnd = new AtomicLong();
        RiskEvaluator riskEvaluator = score -> {
            riskStart.set(System.nanoTime());
            sleepQuietly(80L);
            riskEnd.set(System.nanoTime());
            return score;
        };
        StockReader stockReader = skuCode -> {
            stockStart.set(System.nanoTime());
            sleepQuietly(80L);
            stockEnd.set(System.nanoTime());
            return 5;
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            ParallelPrecheckService service = new ParallelPrecheckService(pool, riskEvaluator, stockReader);
            ParallelPrecheckService.PrecheckResult result = service.check("SKU-PHONE", 1, 15);
            assertThat(result.getRiskScore()).isEqualTo(15);
            assertThat(result.getAvailable()).isEqualTo(5);
            assertThat(result.isStockEnough()).isTrue();
            assertThat(stockStart.get()).isLessThan(riskEnd.get());
            assertThat(riskStart.get()).isLessThan(stockEnd.get());
        } finally {
            pool.shutdownNow();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
