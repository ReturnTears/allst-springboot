package com.allst.springboot.tradeorder.service;

import org.springframework.stereotype.Component;

@Component
public class SimulatedRiskClient implements RiskEvaluator {

    @Override
    public int evaluate(int declaredScore) {
        pause();
        return declaredScore;
    }

    private void pause() {
        try {
            Thread.sleep(80L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
