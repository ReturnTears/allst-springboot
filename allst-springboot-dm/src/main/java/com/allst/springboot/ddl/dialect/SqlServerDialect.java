package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/** SQL Server：方括号标识符、IDENTITY(1,1)、扩展属性做注释。 */
@Component
public class SqlServerDialect extends AbstractDdlDialect {

    public SqlServerDialect() {
        super("SQLSERVER", EnumSet.of(
                Capability.PRIMARY_KEY, Capability.AUTO_INCREMENT,
                Capability.POST_DDL_COMMENT, Capability.TABLE_OPTIONS));
        registerType(STRING, c -> withLength("nvarchar", c));
        registerType(TEXT, c -> "nvarchar(max)");
        registerType(INT, c -> "int");
        registerType(BIGINT, c -> "bigint");
        registerType(UINT, c -> "bigint");
        registerType(DECIMAL, c -> withPrecision("decimal", c));
        registerType(FLOAT, c -> "float");
        registerType(BOOL, c -> "bit");
        registerType(DATE, c -> "date");
        registerType(DATETIME, c -> "datetime2");
        registerType(TIMESTAMP, c -> "datetimeoffset");
        registerType(JSON, c -> "nvarchar(max)");
        registerType(BLOB, c -> "varbinary(max)");
    }

    @Override
    public String quote(String identifier) {
        return "[" + identifier + "]";
    }

    @Override
    public String autoIncrementClause(ColumnMeta column) {
        return column.isAutoIncrement() ? "IDENTITY(1,1)" : "";
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
    public List<String> postDdlStatements(TableMeta table) {
        List<String> out = new ArrayList<>();
        String schema = hasText(table.getSchemaName()) ? table.getSchemaName() : "dbo";
        if (hasText(table.getComment())) {
            out.add(extendedProperty(schema, table.getTableName(), null, table.getComment()));
        }
        for (ColumnMeta c : table.getColumns()) {
            if (hasText(c.getComment())) {
                out.add(extendedProperty(schema, table.getTableName(), c.getColumnName(), c.getComment()));
            }
        }
        return out;
    }

    private String extendedProperty(String schema, String table, String column, String comment) {
        StringBuilder s = new StringBuilder("EXEC sp_addextendedproperty @name=N'MS_Description'")
                .append(", @value=N'").append(literal(comment)).append("'")
                .append(", @level0type=N'SCHEMA', @level0name=N'").append(schema).append("'")
                .append(", @level1type=N'TABLE', @level1name=N'").append(table).append("'");
        if (hasText(column)) {
            s.append(", @level2type=N'COLUMN', @level2name=N'").append(column).append("'");
        }
        return s.toString();
    }
}
