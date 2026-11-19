package com.allst.springboot.ddl.dialect;

import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hadoop 系方言（Impala / Hive）的共同骨架：反引号、分区列单独声明、内联 COMMENT、存储格式后缀。
 * 它们没有主键/自增概念，能力集合里就不声明这两项。
 */
public abstract class AbstractHadoopDialect extends AbstractDdlDialect {

    protected AbstractHadoopDialect(String name, Set<Capability> capabilities) {
        super(name, capabilities);
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
    public String inlineTableComment(TableMeta table) {
        return hasText(table.getComment()) ? "COMMENT '" + literal(table.getComment()) + "'" : "";
    }

    /** Impala/Hive 的分区列不在普通列块里，而是 PARTITIONED BY (col type, ...) 单独声明 */
    @Override
    public String partitionClause(TableMeta table) {
        List<ColumnMeta> parts = table.partitionColumns();
        if (parts.isEmpty()) {
            return "";
        }
        String cols = parts.stream()
                .map(c -> quote(c.getColumnName()) + " " + renderType(c))
                .collect(Collectors.joining(", "));
        return "PARTITIONED BY (" + cols + ")";
    }

    @Override
    public String tableOptionsClause(TableMeta table) {
        return table.getTableOptions();
    }
}
