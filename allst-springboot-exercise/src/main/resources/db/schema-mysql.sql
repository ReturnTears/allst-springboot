-- =====================================================================
-- MySQL 8 DDL，对应 allst-springboot-exercise 模块的持久化实体。
-- 列名按 MyBatis-Flex 默认「驼峰转下划线」映射生成。
-- 字符集 utf8mb4，引擎 InnoDB。
-- 注意：User 为纯展示模型（无 @Table 注解），不建表。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS allst_exercise
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE allst_exercise;

-- ---------------------------------------------------------------------
-- 实体 com.allst.exercise.model.Person  ->  表 person
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS person;
CREATE TABLE person (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    first_name VARCHAR(64)  DEFAULT NULL COMMENT '名',
    last_name  VARCHAR(64)  DEFAULT NULL COMMENT '姓',
    age        INT          DEFAULT NULL COMMENT '年龄',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '人员表';

-- ---------------------------------------------------------------------
-- 实体 com.allst.exercise.model.Customers  ->  表 customers
-- 说明：实体中 @Id(keyType=Auto) 被注释，id 由业务赋值，故此处不自增。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS customers;
CREATE TABLE customers (
    id      BIGINT       NOT NULL COMMENT '主键（业务赋值）',
    name    VARCHAR(64)  DEFAULT NULL COMMENT '姓名',
    email   VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    address VARCHAR(255) DEFAULT NULL COMMENT '地址',
    city    VARCHAR(64)  DEFAULT NULL COMMENT '城市',
    country VARCHAR(64)  DEFAULT NULL COMMENT '国家',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客户表';

-- ---------------------------------------------------------------------
-- 实体 com.allst.exercise.model.Account  ->  表 tb_account
-- 说明：@Id(keyType=Auto) 生效，id 自增。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS tb_account;
CREATE TABLE tb_account (
    id        BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_name VARCHAR(64) DEFAULT NULL COMMENT '用户名',
    age       INT         DEFAULT NULL COMMENT '年龄',
    birthday  DATETIME    DEFAULT NULL COMMENT '生日',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '账户表';
