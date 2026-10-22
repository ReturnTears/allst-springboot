package com.allst.springboot.domain.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String remark;
    private OrderStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
}
