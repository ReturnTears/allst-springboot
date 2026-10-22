CREATE TABLE IF NOT EXISTS t_product (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(128)   NOT NULL,
    price       DECIMAL(12, 2) NOT NULL,
    stock       INT            NOT NULL,
    detail      VARCHAR(2000)  NULL,
    category    VARCHAR(64)    NOT NULL,
    updated_at  TIMESTAMP      NULL
);

CREATE TABLE IF NOT EXISTS t_page (
    id          BIGINT PRIMARY KEY,
    slug        VARCHAR(64)    NOT NULL,
    title       VARCHAR(200)   NOT NULL,
    content     VARCHAR(4000)  NOT NULL,
    updated_at  TIMESTAMP      NULL,
    CONSTRAINT uk_page_slug UNIQUE (slug)
);

CREATE TABLE IF NOT EXISTS t_behavior (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(64)  NOT NULL,
    item_id     BIGINT       NOT NULL,
    action      VARCHAR(32)  NOT NULL,
    score       INT          NOT NULL,
    created_at  TIMESTAMP    NULL
);
