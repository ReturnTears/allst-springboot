package com.allst.springboot.tradeorder.service;

import com.allst.springboot.tradeorder.repo.SkuStockRepository;
import org.springframework.stereotype.Component;

@Component
public class SimulatedStockQuery implements StockReader {

    private final SkuStockRepository stockRepository;

    public SimulatedStockQuery(SkuStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public int available(String skuCode) {
        pause();
        return stockRepository.findBySkuCode(skuCode).map(stock -> stock.getAvailable()).orElse(0);
    }

    private void pause() {
        try {
            Thread.sleep(80L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
