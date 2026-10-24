-- 可重复执行的演示库脚本。原 init.sql 保留不动，里面有 DELETE，不适合反复运行。
-- 连接：MySQL 8，用户 root，密码为空。

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE demo;

CREATE TABLE IF NOT EXISTS book
(
    id        INT          NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255) NULL,
    pageCount VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY index_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS author
(
    id        INT          NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(255) NULL,
    lastName  VARCHAR(255) NULL,
    bookId    INT          NULL,
    PRIMARY KEY (id),
    UNIQUE KEY index_author_name (firstName),
    KEY fk_author_book (bookId),
    CONSTRAINT fk_author_book FOREIGN KEY (bookId) REFERENCES book (id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS category
(
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(32)  NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS reader
(
    id         INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(32)  NOT NULL,
    email      VARCHAR(128) NOT NULL,
    phone      VARCHAR(20)  NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reader_username (username),
    UNIQUE KEY uk_reader_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS book_category
(
    book_id     INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (book_id),
    KEY fk_bc_category (category_id),
    CONSTRAINT fk_bc_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
    CONSTRAINT fk_bc_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS membership
(
    id         INT            NOT NULL AUTO_INCREMENT,
    reader_id  INT            NOT NULL,
    book_id    INT            NOT NULL,
    plan       VARCHAR(32)    NOT NULL,
    status     VARCHAR(32)    NOT NULL,
    start_date DATE           NOT NULL,
    end_date   DATE           NOT NULL,
    price      DECIMAL(10, 2) NOT NULL,
    remark     VARCHAR(255)   NULL,
    created_at DATETIME       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_membership_reader (reader_id),
    KEY idx_membership_book (book_id),
    CONSTRAINT fk_membership_reader FOREIGN KEY (reader_id) REFERENCES reader (id),
    CONSTRAINT fk_membership_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT IGNORE INTO book (id, name, pageCount)
VALUES (1, 'the golden ticket', '255'),
       (2, 'coding game', '400'),
       (3, 'java', '500'),
       (4, 'c++', '650'),
       (5, 'scala', '700'),
       (6, 'python', '800'),
       (7, 'go', '1000');

INSERT IGNORE INTO author (id, firstName, lastName, bookId)
VALUES (1, 'J. R. R.', 'Tolkien', 1),
       (2, 'Joanne', 'Rowling', 2),
       (3, 'J. K.', 'Rowling', 3),
       (4, 'Stephen', 'King', 4),
       (5, 'Charles', 'Dickens', 5),
       (6, 'Jane', 'Austen', 6),
       (7, 'Leo', 'Tolstoy', 7);

INSERT IGNORE INTO category (id, name, description)
VALUES (1, '文学', '小说、散文和随笔'),
       (2, '计算机', '语言、工程和练习'),
       (3, '科幻', '架上暂时还没有归到这里的书');

INSERT IGNORE INTO book_category (book_id, category_id)
VALUES (1, 1),
       (2, 2),
       (3, 2),
       (4, 2),
       (5, 2),
       (6, 2);

INSERT IGNORE INTO reader (id, username, email, phone, status, created_at)
VALUES (1, '林夏', 'linxia@example.com', '13800000001', 'ACTIVE', '2026-09-02 09:10:00'),
       (2, '周衡', 'zhouheng@example.com', '13800000002', 'ACTIVE', '2026-09-18 14:20:00'),
       (3, '苏晚', 'suwan@example.com', NULL, 'ACTIVE', '2026-10-01 11:05:00'),
       (4, '陈予安', 'chenyuan@example.com', '13800000004', 'DISABLED', '2026-10-12 16:40:00');

INSERT IGNORE INTO membership (id, reader_id, book_id, plan, status, start_date, end_date, price, remark, created_at)
VALUES (1, 1, 3, 'MONTHLY', 'ACTIVE', '2026-10-01', '2026-10-31', 18.00, '备考', '2026-10-01 09:30:00'),
       (2, 1, 6, 'YEARLY', 'ACTIVE', '2026-01-15', '2027-01-15', 168.00, '长期放在案头', '2026-01-15 10:00:00'),
       (3, 2, 1, 'QUARTERLY', 'PAUSED', '2026-09-01', '2026-11-30', 48.00, '出差暂停', '2026-09-01 08:15:00'),
       (4, 3, 5, 'MONTHLY', 'CANCELLED', '2026-08-01', '2026-08-31', 18.00, '改读别的', '2026-08-01 13:00:00'),
       (5, 2, 7, 'MONTHLY', 'EXPIRED', '2026-06-01', '2026-07-01', 18.00, NULL, '2026-06-01 09:00:00');

-- 分类关系跟着书走。已有库重复执行时，把外键改成级联删除。
ALTER TABLE book_category DROP FOREIGN KEY fk_bc_book;
ALTER TABLE book_category ADD CONSTRAINT fk_bc_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE;
