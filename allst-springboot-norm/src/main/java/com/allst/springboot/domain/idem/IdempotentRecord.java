package com.allst.springboot.domain.idem;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 幂等记录。idem_key 由 appId 和调用方幂等键组成，全局唯一。
 */
@Getter
@Setter
@Entity
@Table(name = "norm_idempotent", uniqueConstraints = {
        @UniqueConstraint(name = "uk_norm_idempotent_key", columnNames = "idem_key")
})
public class IdempotentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idem_key", nullable = false, length = 128)
    private String idemKey;

    @Column(name = "app_id", length = 64)
    private String appId;

    @Column(nullable = false, length = 255)
    private String api;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotentStatus status;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
