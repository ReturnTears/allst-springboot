package com.allst.springboot.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 落库用的订单记录。运行环境是 MySQL 8（root / 空密码、库 allst_dm，ddl-auto=update），
 * 测试环境用 H2 内存库。仓储接口见 OrderRecordRepository。
 */
@Entity
@Table(name = "dm_order")
@Getter
@Setter
@NoArgsConstructor
public class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, length = 64)
    private String orderNo;

    @Column(name = "buyer", length = 64)
    private String buyer;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "original_amount", precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "discount", precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "payable", precision = 12, scale = 2)
    private BigDecimal payable;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private OrderStatus status;

    @Column(name = "pay_channel", length = 32)
    private String payChannel;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "created_at")
    private Instant createdAt;
}
