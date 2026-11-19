package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * Apache Doris：主键用 UNIQUE KEY 表达（不是标准 PRIMARY KEY），
 * 分区 + DISTRIBUTED BY 通过表选项承载，无自增。
 */
@Component
public class DorisDialect extends AbstractDdlDialect {

    public DorisDialect() {
        super("DORIS", EnumSet.of(
                Capability.PRIMARY_KEY, Capability.PARTITION,
                Capability.TABLE_OPTIONS, Capability.INLINE_COLUMN_COMMENT,
                Capability.IF_NOT_EXISTS));
        registerType(STRING, c -> c.getLength() != null ? withLength("varchar", c) : "string");
        registerType(TEXT, c -> "string");
        registerType(INT, c -> "int");
        registerType(BIGINT, c -> "bigint");
        registerType(UINT, c -> "bigint");
        registerType(DECIMAL, c -> withPrecision("decimal", c));
        registerType(FLOAT, c -> "double");
        registerType(BOOL, c -> "boolean");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "datetime");
        registerType(TIMESTAMP, c -> "datetime");
        registerType(JSON, c -> "string");
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
    public String primaryKeyClause(TableMeta table) {
        List<ColumnMeta> pk = table.primaryKeyColumns();
        if (pk.isEmpty()) {
            return "";
        }
        return "UNIQUE KEY (" + pk.stream().map(c -> quote(c.getColumnName()))
                .collect(Collectors.joining(", ")) + ")";
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
