package com.allst.springboot.ddl.dialect;

/**
 * 方言能力。DDL 差异的本质不是「类型名不同」，而是「片段有无」：
 * OLTP 库有主键/自增，OLAP 引擎有分区/存储格式，PG/Oracle 的列注释是独立语句。
 *
 * <p>把差异抽象成一组能力，模板方法按能力决定渲染哪些片段，
 * 新增方言只要声明自己支持哪些能力、实现对应钩子，不必重写整个生成流程。
 */
public enum Capability {
    /** 支持表级 PRIMARY KEY(...) 约束 */
    PRIMARY_KEY,
    /** 支持列级自增（AUTO_INCREMENT / SERIAL / IDENTITY） */
    AUTO_INCREMENT,
    /** 列注释内联在列定义里：col type COMMENT 'x' */
    INLINE_COLUMN_COMMENT,
    /** 表注释内联在 CREATE 语句里：) COMMENT='x' */
    INLINE_TABLE_COMMENT,
    /** 需要独立的 COMMENT ON / sp_addextendedproperty 语句 */
    POST_DDL_COMMENT,
    /** 支持分区子句 */
    PARTITION,
    /** 分区列在 CREATE 列定义块之外单独声明（Impala/Hive） */
    PARTITION_COLUMNS_SEPARATE,
    /** 支持表选项后缀（ENGINE / TABLESPACE / STORED AS） */
    TABLE_OPTIONS,
    /** 支持 CREATE TABLE IF NOT EXISTS */
    IF_NOT_EXISTS
}
