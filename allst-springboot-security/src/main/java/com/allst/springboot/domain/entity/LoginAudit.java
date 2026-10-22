package com.allst.springboot.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 登录成败记录。不保存口令。见 docs/01-authentication.md。
 */
@Getter
@Setter
@Entity
@Table(name = "sec_login_audit")
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 256)
    private String userAgent;

    @Column(nullable = false, length = 128)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
