package com.allst.springboot.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 已注销的访问令牌 jti。过期后由定时任务删除，避免表无限增长。见 docs/09-jwt.md。
 */
@Getter
@Setter
@Entity
@Table(name = "sec_token_blacklist")
public class TokenBlacklist {

    @Id
    @Column(nullable = false, length = 64)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
