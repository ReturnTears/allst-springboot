package com.allst.springboot.tradeorder.service;

import com.allst.springboot.tradeorder.repo.SkuStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final SkuStockRepository stockRepository;

    public StockService(SkuStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean exists(String skuCode) {
        return stockRepository.existsBySkuCode(skuCode);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryDeduct(String skuCode, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        return stockRepository.deductIfEnough(skuCode, quantity) == 1;
    }
}
