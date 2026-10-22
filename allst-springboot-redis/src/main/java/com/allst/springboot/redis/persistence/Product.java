package com.allst.springboot.redis.persistence;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String detail;
    private String category;
    private LocalDateTime updatedAt;
}
