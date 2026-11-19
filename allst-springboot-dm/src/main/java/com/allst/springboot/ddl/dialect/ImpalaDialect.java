package com.allst.springboot.ddl.dialect;

import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * Apache Impala：外部表 + 分区 + PARQUET，无主键/自增。
 * BLOB 在 Impala 没有对应物理类型——不注册，元数据里写了就明确报错。
 */
@Component
public class ImpalaDialect extends AbstractHadoopDialect {

    public ImpalaDialect() {
        super("IMPALA", EnumSet.of(
                Capability.PARTITION, Capability.PARTITION_COLUMNS_SEPARATE,
                Capability.TABLE_OPTIONS, Capability.INLINE_COLUMN_COMMENT,
                Capability.INLINE_TABLE_COMMENT));
        // Impala 的 STRING 是变长类型，不接受 STRING(n)；要限长应使用 VARCHAR 逻辑类型
        registerType(STRING, c -> "string");
        registerType(TEXT, c -> "string");
        registerType(INT, c -> "int");
        registerType(BIGINT, c -> "bigint");
        registerType(UINT, c -> "bigint");
        registerType(DECIMAL, c -> withPrecision("decimal", c));
        registerType(FLOAT, c -> "double");
        registerType(BOOL, c -> "boolean");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "timestamp");
        registerType(TIMESTAMP, c -> "timestamp");
        registerType(JSON, c -> "string");
    }
}
