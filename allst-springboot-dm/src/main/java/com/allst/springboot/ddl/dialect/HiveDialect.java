package com.allst.springboot.ddl.dialect;

import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/** Apache Hive：与 Impala 同构，支持 IF NOT EXISTS，BLOB 对应 binary。 */
@Component
public class HiveDialect extends AbstractHadoopDialect {

    public HiveDialect() {
        super("HIVE", EnumSet.of(
                Capability.PARTITION, Capability.PARTITION_COLUMNS_SEPARATE,
                Capability.TABLE_OPTIONS, Capability.INLINE_COLUMN_COMMENT,
                Capability.INLINE_TABLE_COMMENT, Capability.IF_NOT_EXISTS));
        // Hive 的 STRING 同样是变长类型，不写长度
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
        registerType(BLOB, c -> "binary");
    }
}
