-- 表结构参考。默认由 JPA ddl-auto=update 建表，本文件便于评审和手工初始化。
-- 手机号、身份证号以 AES-GCM 密文存储，列宽留有余量。

CREATE TABLE IF NOT EXISTS norm_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(32)  NOT NULL,
    real_name   VARCHAR(32)  NOT NULL,
    phone       VARCHAR(512) NOT NULL,
    email       VARCHAR(128) NOT NULL,
    id_card     VARCHAR(512) NOT NULL,
    status      INT          NOT NULL,
    created_by  VARCHAR(64)  NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    CONSTRAINT uk_norm_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS norm_order (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no    VARCHAR(32)    NOT NULL,
    user_id     BIGINT         NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    remark      VARCHAR(200)   NULL,
    status      VARCHAR(32)    NOT NULL,
    created_by  VARCHAR(64)    NULL,
    created_at  DATETIME       NOT NULL,
    updated_at  DATETIME       NOT NULL,
    CONSTRAINT uk_norm_order_no UNIQUE (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS norm_api_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id      VARCHAR(64)   NULL,
    app_id        VARCHAR(64)   NULL,
    client_ip     VARCHAR(64)   NULL,
    http_method   VARCHAR(16)   NOT NULL,
    uri           VARCHAR(255)  NOT NULL,
    status_code   INT           NOT NULL,
    cost_ms       BIGINT        NOT NULL,
    request_body  VARCHAR(2000) NULL,
    response_body VARCHAR(500)  NULL,
    created_at    DATETIME      NOT NULL,
    INDEX idx_api_log_trace (trace_id),
    INDEX idx_api_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS norm_idempotent (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    idem_key      VARCHAR(128) NOT NULL,
    app_id        VARCHAR(64)  NULL,
    api           VARCHAR(255) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    http_status   INT          NULL,
    response_json LONGTEXT     NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NULL,
    CONSTRAINT uk_norm_idempotent_key UNIQUE (idem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
