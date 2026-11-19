package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * 通用兜底方言：ANSI SQL 近似写法。
 * 未注册的方言名会路由到这里，保证「没见过的库」也能导出可读 DDL，
 * 上层通过 DialectRouter.isFallback() 提示这是降级输出。
 */
@Component
public class GenericDialect extends AbstractDdlDialect {

    public GenericDialect() {
        super("GENERIC", EnumSet.of(Capability.PRIMARY_KEY, Capability.TABLE_OPTIONS));
        registerType(STRING, c -> withLength("varchar", c));
        registerType(TEXT, c -> "varchar(4000)");
        registerType(INT, c -> "integer");
        registerType(BIGINT, c -> "bigint");
        registerType(UINT, c -> "bigint");
        registerType(DECIMAL, c -> withPrecision("decimal", c));
        registerType(FLOAT, c -> "double precision");
        registerType(BOOL, c -> "boolean");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "timestamp");
        registerType(TIMESTAMP, c -> "timestamp");
        registerType(JSON, c -> "clob");
        registerType(BLOB, c -> "blob");
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier + "\"";
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
}
