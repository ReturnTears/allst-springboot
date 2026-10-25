package com.allst.springboot.tradeorder.service;

import com.allst.springboot.tradeorder.domain.SkuStock;
import com.allst.springboot.tradeorder.repo.SkuStockRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DemoStockInitializer implements ApplicationRunner {

    private final SkuStockRepository stockRepository;

    public DemoStockInitializer(SkuStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensure("SKU-PHONE", 100);
        ensure("SKU-GIFT", 2);
    }

    private void ensure(String skuCode, int available) {
        if (stockRepository.existsBySkuCode(skuCode)) {
            return;
        }
        SkuStock stock = new SkuStock();
        stock.setSkuCode(skuCode);
        stock.setAvailable(available);
        stockRepository.save(stock);
    }
}
