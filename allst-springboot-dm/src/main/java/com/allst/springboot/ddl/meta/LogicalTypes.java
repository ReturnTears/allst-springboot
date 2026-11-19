package com.allst.springboot.ddl.meta;

/**
 * 通用逻辑类型常量。元数据表里存的是「逻辑类型」字符串，不是物理类型——
 * 同一个 STRING 在 MySQL 是 varchar、在 Oracle 是 varchar2、在 ClickHouse 是 String。
 *
 * <p>这里只给出约定俗成的名字，方言可以扩展自己的逻辑类型（如 ClickHouse 的 UINT64）。
 * 方言通过 {@code supports(logicalType)} 声明自己认识哪些类型，不认识就明确报错，
 * 绝不悄悄猜一个物理类型——这是「类型体系方言自治」的落点。
 */
public final class LogicalTypes {

    public static final String STRING = "STRING";
    public static final String TEXT = "TEXT";
    public static final String INT = "INT";
    public static final String BIGINT = "BIGINT";
    public static final String UINT = "UINT";
    public static final String DECIMAL = "DECIMAL";
    public static final String FLOAT = "FLOAT";
    public static final String BOOL = "BOOL";
    public static final String DATE = "DATE";
    public static final String DATETIME = "DATETIME";
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String JSON = "JSON";
    public static final String BLOB = "BLOB";

    private LogicalTypes() {
    }
}
