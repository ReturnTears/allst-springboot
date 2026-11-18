package com.allst.boot.repository;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表。刻意<b>不建 JPA 关联</b>（不用 @ManyToOne），
 * 因为本模块要演示的正是「手动批量查 + Stream 建索引关联」如何替代懒加载引发的 N+1。
 * <p>
 * 参考 allst-springboot-security 的实体风格：Lombok + 显式列名。
 */
@Getter
@Setter
@Entity
@Table(name = "stream_order")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 16)
    private String channel;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 32)
    private String city;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}