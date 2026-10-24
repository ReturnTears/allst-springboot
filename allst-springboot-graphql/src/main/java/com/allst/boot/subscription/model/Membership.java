package com.allst.boot.subscription.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "membership", schema = "demo")
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reader_id", nullable = false)
    private Integer readerId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Double price;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = MembershipStatus.ACTIVE;
        }
    }
}
