package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/** MySQL 8：反引号、AUTO_INCREMENT、内联 COMMENT、表选项。 */
@Component
public class MySqlDialect extends AbstractDdlDialect {

    public MySqlDialect() {
        super("MYSQL", EnumSet.of(
                Capability.PRIMARY_KEY, Capability.AUTO_INCREMENT,
                Capability.INLINE_COLUMN_COMMENT, Capability.INLINE_TABLE_COMMENT,
                Capability.PARTITION, Capability.TABLE_OPTIONS, Capability.IF_NOT_EXISTS));
        registerType(STRING, c -> withLength("varchar", c));
        registerType(TEXT, c -> "text");
        registerType(INT, c -> "int");
        registerType(BIGINT, c -> "bigint");
        registerType(UINT, c -> "int unsigned");
        registerType(DECIMAL, c -> withPrecision("decimal", c));
        registerType(FLOAT, c -> "float");
        registerType(BOOL, c -> "tinyint(1)");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "datetime");
        registerType(TIMESTAMP, c -> "timestamp");
        registerType(JSON, c -> "json");
        registerType(BLOB, c -> "blob");
    }

    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String autoIncrementClause(ColumnMeta column) {
        return column.isAutoIncrement() ? "AUTO_INCREMENT" : "";
    }

    @Override
    public String inlineColumnComment(ColumnMeta column) {
        return hasText(column.getComment()) ? "COMMENT '" + literal(column.getComment()) + "'" : "";
    }

    @Override
    public String inlineTableComment(TableMeta table) {
        return hasText(table.getComment()) ? "COMMENT='" + literal(table.getComment()) + "'" : "";
    }

    @Override
    public String primaryKeyClause(TableMeta table) {
        List<ColumnMeta> pk = table.primaryKeyColumns();
        if (pk.isEmpty()) {
            return "";
        }
        return "PRIMARY KEY (" + pk.stream().map(c -> quote(c.getColumnName()))
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String tableOptionsClause(TableMeta table) {
        return table.getTableOptions();
    }

    @Override
    public String partitionClause(TableMeta table) {
        List<ColumnMeta> parts = table.partitionColumns();
        if (parts.isEmpty()) {
            return "";
        }
        String cols = parts.stream().map(c -> quote(c.getColumnName()))
                .collect(Collectors.joining(", "));
        return "PARTITION BY HASH (" + cols + ") PARTITIONS 4";
    }
}
