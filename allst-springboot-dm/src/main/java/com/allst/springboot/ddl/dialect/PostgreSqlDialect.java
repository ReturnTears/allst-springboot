package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * PostgreSQL：双引号、SERIAL/BIGSERIAL、COMMENT ON 独立语句。
 * 列注释不是内联片段而是 post DDL，这是能力模型里 POST_DDL_COMMENT 的典型场景。
 */
@Component
public class PostgreSqlDialect extends AbstractDdlDialect {

    public PostgreSqlDialect() {
        super("POSTGRESQL", EnumSet.of(
                Capability.PRIMARY_KEY, Capability.AUTO_INCREMENT,
                Capability.POST_DDL_COMMENT, Capability.IF_NOT_EXISTS));
        registerType(STRING, c -> withLength("varchar", c));
        registerType(TEXT, c -> "text");
        registerType(INT, c -> c.isAutoIncrement() ? "serial" : "integer");
        registerType(BIGINT, c -> c.isAutoIncrement() ? "bigserial" : "bigint");
        registerType(UINT, c -> "bigint");
        registerType(DECIMAL, c -> withPrecision("numeric", c));
        registerType(FLOAT, c -> "double precision");
        registerType(BOOL, c -> "boolean");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "timestamp");
        registerType(TIMESTAMP, c -> "timestamp");
        registerType(JSON, c -> "jsonb");
        registerType(BLOB, c -> "bytea");
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
    public List<String> postDdlStatements(TableMeta table) {
        List<String> statements = new ArrayList<>();
        String qualified = qualifiedName(table);
        if (hasText(table.getComment())) {
            statements.add("COMMENT ON TABLE " + qualified + " IS '" + literal(table.getComment()) + "'");
        }
        for (ColumnMeta c : table.getColumns()) {
            if (hasText(c.getComment())) {
                statements.add("COMMENT ON COLUMN " + qualified + "." + quote(c.getColumnName())
                        + " IS '" + literal(c.getComment()) + "'");
            }
        }
        return statements;
    }
}
