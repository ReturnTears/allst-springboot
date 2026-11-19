package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * SQLite：自增绑定在 INTEGER PRIMARY KEY 列上，没有独立的表级自增语法。
 * 字符串/时间都用 TEXT 或 NUMERIC 亲和类型，是「弱类型」方言的代表。
 */
@Component
public class SqliteDialect extends AbstractDdlDialect {

    public SqliteDialect() {
        super("SQLITE", EnumSet.of(
                Capability.PRIMARY_KEY, Capability.AUTO_INCREMENT, Capability.IF_NOT_EXISTS));
        registerType(STRING, c -> "text");
        registerType(TEXT, c -> "text");
        registerType(INT, c -> "integer");
        registerType(BIGINT, c -> "integer");
        registerType(UINT, c -> "integer");
        registerType(DECIMAL, c -> "numeric");
        registerType(FLOAT, c -> "real");
        registerType(BOOL, c -> "integer");
        registerType(DATE, c -> "text");
        registerType(DATETIME, c -> "text");
        registerType(TIMESTAMP, c -> "text");
        registerType(JSON, c -> "text");
        registerType(BLOB, c -> "blob");
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String autoIncrementClause(ColumnMeta column) {
        return column.isAutoIncrement() ? "PRIMARY KEY AUTOINCREMENT" : "";
    }

    @Override
    public String primaryKeyClause(TableMeta table) {
        List<ColumnMeta> pk = table.primaryKeyColumns();
        if (pk.isEmpty() || pk.stream().anyMatch(ColumnMeta::isAutoIncrement)) {
            return "";
        }
        return "PRIMARY KEY (" + pk.stream().map(c -> quote(c.getColumnName()))
                .collect(Collectors.joining(", ")) + ")";
    }
}
