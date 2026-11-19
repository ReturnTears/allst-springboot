package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * ClickHouse：MergeTree 引擎、PARTITION BY、强类型（Int32/Int64/UInt8/DateTime）。
 * 分区列仍在普通列块里，PARTITION BY 只引用列名——与 Impala 的「分区列单独声明」不同。
 */
@Component
public class ClickHouseDialect extends AbstractDdlDialect {

    public ClickHouseDialect() {
        super("CLICKHOUSE", EnumSet.of(
                Capability.PARTITION, Capability.TABLE_OPTIONS,
                Capability.INLINE_COLUMN_COMMENT, Capability.IF_NOT_EXISTS));
        registerType(STRING, c -> "String");
        registerType(TEXT, c -> "String");
        registerType(INT, c -> "Int32");
        registerType(BIGINT, c -> "Int64");
        registerType(UINT, c -> "UInt64");
        registerType(DECIMAL, c -> withPrecision("Decimal", c));
        registerType(FLOAT, c -> "Float64");
        registerType(BOOL, c -> "UInt8");
        registerType(DATE, c -> "Date");
        registerType(DATETIME, c -> "DateTime");
        registerType(TIMESTAMP, c -> "DateTime64(3)");
        registerType(JSON, c -> "String");
        registerType(BLOB, c -> "String");
    }

    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String inlineColumnComment(ColumnMeta column) {
        return hasText(column.getComment()) ? "COMMENT '" + literal(column.getComment()) + "'" : "";
    }

    @Override
    public String partitionClause(TableMeta table) {
        List<ColumnMeta> parts = table.partitionColumns();
        if (parts.isEmpty()) {
            return "";
        }
        return "PARTITION BY (" + parts.stream().map(c -> quote(c.getColumnName()))
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String tableOptionsClause(TableMeta table) {
        return table.getTableOptions();
    }
}
