package com.allst.springboot.tradeorder.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ParallelPrecheckService {

    private final Executor executor;
    private final RiskEvaluator riskEvaluator;
    private final StockReader stockReader;

    public ParallelPrecheckService(@Qualifier("stateMachineExecutor") Executor executor,
                                   RiskEvaluator riskEvaluator,
                                   StockReader stockReader) {
        this.executor = executor;
        this.riskEvaluator = riskEvaluator;
        this.stockReader = stockReader;
    }

    public PrecheckResult check(String skuCode, int quantity, int declaredRisk) {
        long startedAt = System.nanoTime();
        CompletableFuture<Integer> riskFuture = CompletableFuture.supplyAsync(
                () -> riskEvaluator.evaluate(declaredRisk), executor);
        CompletableFuture<Integer> stockFuture = CompletableFuture.supplyAsync(
                () -> stockReader.available(skuCode), executor);
        int riskScore = riskFuture.join();
        int available = stockFuture.join();
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return new PrecheckResult(riskScore, available, available >= quantity, elapsedMillis);
    }

    public static final class PrecheckResult {
        private final int riskScore;
        private final int available;
        private final boolean stockEnough;
        private final long elapsedMillis;

        public PrecheckResult(int riskScore, int available, boolean stockEnough, long elapsedMillis) {
            this.riskScore = riskScore;
            this.available = available;
            this.stockEnough = stockEnough;
            this.elapsedMillis = elapsedMillis;
        }

        public int getRiskScore() {
            return riskScore;
        }

        public int getAvailable() {
            return available;
        }

        public boolean isStockEnough() {
            return stockEnough;
        }

        public long getElapsedMillis() {
            return elapsedMillis;
        }
    }
}
