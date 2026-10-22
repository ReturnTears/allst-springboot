-- MySQL 8 参考表结构，与 JPA 实体对应。
-- 示例默认使用 ddl-auto=update。生产环境建议改为 validate，并由迁移工具执行本脚本。

CREATE TABLE IF NOT EXISTS sec_user (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    username                VARCHAR(64)  NOT NULL,
    password                VARCHAR(100) NOT NULL,
    display_name            VARCHAR(64)  NOT NULL,
    enabled                 BIT(1)       NOT NULL,
    account_non_locked      BIT(1)       NOT NULL,
    account_non_expired     BIT(1)       NOT NULL,
    failed_attempts         INT          NOT NULL,
    lock_until              DATETIME(6)  NULL,
    password_changed_at     DATETIME(6)  NULL,
    created_at              DATETIME(6)  NULL,
    updated_at              DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sec_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_role (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    code  VARCHAR(64) NOT NULL,
    name  VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sec_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_permission (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    code  VARCHAR(64) NOT NULL,
    name  VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sec_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_password_history (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pwd_history_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    jti         VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BIT(1)       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_jti (jti),
    KEY idx_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_token_blacklist (
    jti         VARCHAR(64) NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sec_login_audit (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    success     BIT(1)       NOT NULL,
    ip          VARCHAR(64)  NULL,
    user_agent  VARCHAR(256) NULL,
    message     VARCHAR(128) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_login_audit_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
