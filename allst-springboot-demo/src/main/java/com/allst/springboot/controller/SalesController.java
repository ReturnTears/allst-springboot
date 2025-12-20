package com.allst.springboot.controller;

import com.allst.springboot.entity.SalesSummary;
import com.allst.springboot.service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hutu
 * @since 2025-10-31 下午 10:05
 */
@RestController
@RequestMapping("/sales")
public class SalesController {
    @Autowired
    private SalesService salesService;

    @GetMapping("/summary")
    public SalesSummary fetchSummary() {
        return salesService.fetchSummary();
    }
}
