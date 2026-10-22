package com.allst.springboot.domain.user;

import com.allst.springboot.crypto.AesStringConverter;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 用户。手机号和身份证号以密文落库，内存中是明文，返回前由 VO 脱敏。
 */
@Getter
@Setter
@Entity
@Table(name = "norm_user", uniqueConstraints = {
        @UniqueConstraint(name = "uk_norm_user_username", columnNames = "username")
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(name = "real_name", nullable = false, length = 32)
    private String realName;

    @Convert(converter = AesStringConverter.class)
    @Column(nullable = false, length = 512)
    private String phone;

    @Column(nullable = false, length = 128)
    private String email;

    @Convert(converter = AesStringConverter.class)
    @Column(name = "id_card", nullable = false, length = 512)
    private String idCard;

    /** 1 启用，0 停用。状态变更走单独接口。 */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
