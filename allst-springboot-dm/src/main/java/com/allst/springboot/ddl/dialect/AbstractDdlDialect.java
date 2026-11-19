package com.allst.springboot.ddl.dialect;

import com.allst.springboot.common.BizException;
import com.allst.springboot.ddl.meta.ColumnMeta;
import com.allst.springboot.ddl.meta.TableMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 模板方法基类：DDL 生成骨架只写一份，方言子类只填「能力钩子」。
 *
 * <p>固定流程：校验元数据 → 分流普通列/分区列 → 逐列渲染 → 主键 → 表级子句
 * （表注释 / 分区 / 表选项）→ 拼装主 DDL → 附加语句。
 * generate() 声明为 final，子类不能改顺序——这正是 ReceiptTemplate 的套路。
 *
 * <p>类型体系方言自治：子类用 registerType() 注册自己的「逻辑类型 → 物理类型」渲染器，
 * 未注册的类型直接报错，不悄悄降级。
 */
public abstract class AbstractDdlDialect implements DdlDialect {

    private final String name;
    private final Set<Capability> capabilities;
    private final Map<String, Function<ColumnMeta, String>> typeRenderers = new LinkedHashMap<>();

    protected AbstractDdlDialect(String name, Set<Capability> capabilities) {
        this.name = name;
        this.capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    /** 注册逻辑类型渲染器，构造方言时调用 */
    protected final void registerType(String logicalType, Function<ColumnMeta, String> renderer) {
        typeRenderers.put(logicalType.toUpperCase(java.util.Locale.ROOT), renderer);
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final Set<Capability> capabilities() {
        return capabilities;
    }

    @Override
    public boolean supportsType(String logicalType) {
        return logicalType != null && typeRenderers.containsKey(normalizeType(logicalType));
    }

    @Override
    public List<String> supportedTypes() {
        return new ArrayList<>(typeRenderers.keySet());
    }

    @Override
    public String renderType(ColumnMeta column) {
        Function<ColumnMeta, String> renderer = typeRenderers.get(normalizeType(column.getLogicalType()));
        if (renderer == null) {
            throw new BizException(400, "方言 " + name + " 不支持逻辑类型 " + column.getLogicalType()
                    + "（列 " + column.getColumnName() + "）");
        }
        return renderer.apply(column);
    }

    @Override
    public final DdlOutput generate(TableMeta table) {
        List<String> warnings = new ArrayList<>();
        validate(table, warnings);

        List<ColumnMeta> bodyColumns = regularColumns(table);
        List<String> body = new ArrayList<>();
        for (ColumnMeta c : bodyColumns) {
            body.add("  " + renderColumn(c));
        }
        addIfText(body, primaryKeyClause(table));

        List<String> clauses = new ArrayList<>();
        clauses.add("(\n" + String.join(",\n", body) + "\n)");
        addIfText(clauses, inlineTableComment(table));
        addIfText(clauses, partitionClause(table));
        addIfText(clauses, tableOptionsClause(table));

        StringBuilder sql = new StringBuilder(createPrefix(table))
                .append(" ").append(qualifiedName(table));
        for (String clause : clauses) {
            sql.append("\n").append(clause);
        }
        sql.append(";");
        for (String stmt : postDdlStatements(table)) {
            if (hasText(stmt)) {
                String s = stmt.trim();
                sql.append("\n").append(s.endsWith(";") ? s : s + ";");
            }
        }
        return new DdlOutput(sql.toString(), warnings);
    }

    /** 分区列在 CREATE 列块之外单独声明的方言（Impala/Hive）要把它们从列块剔除 */
    private List<ColumnMeta> regularColumns(TableMeta table) {
        boolean separate = supports(Capability.PARTITION_COLUMNS_SEPARATE);
        List<ColumnMeta> out = new ArrayList<>();
        for (ColumnMeta c : table.getColumns()) {
            if (!separate || !c.isPartition()) {
                out.add(c);
            }
        }
        out.sort((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()));
        return out;
    }

    protected String renderColumn(ColumnMeta c) {
        StringBuilder sb = new StringBuilder(quote(c.getColumnName()))
                .append(" ").append(renderType(c));
        if (!c.isNullable()) {
            sb.append(" NOT NULL");
        }
        String auto = autoIncrementClause(c);
        if (hasText(auto)) {
            sb.append(" ").append(auto.trim());
        }
        if (hasText(c.getDefaultValue())) {
            sb.append(" DEFAULT ").append(c.getDefaultValue().trim());
        }
        String comment = inlineColumnComment(c);
        if (hasText(comment)) {
            sb.append(" ").append(comment.trim());
        }
        return sb.toString();
    }

    protected String createPrefix(TableMeta table) {
        String base = table.isExternalTable() ? "CREATE EXTERNAL TABLE" : "CREATE TABLE";
        return supports(Capability.IF_NOT_EXISTS) ? base + " IF NOT EXISTS" : base;
    }

    protected String qualifiedName(TableMeta table) {
        String tableName = quote(table.getTableName());
        return hasText(table.getSchemaName())
                ? quote(table.getSchemaName()) + "." + tableName
                : tableName;
    }

    private void validate(TableMeta table, List<String> warnings) {
        if (table.getColumns().isEmpty()) {
            throw new IllegalArgumentException("表没有列定义: " + table.getTableName());
        }
        long autoCount = 0;
        for (ColumnMeta c : table.getColumns()) {
            if (c.isAutoIncrement()) {
                autoCount++;
            }
        }
        if (autoCount > 1) {
            throw new IllegalArgumentException("最多只能有一个自增列: " + table.getTableName());
        }
        if (!table.partitionColumns().isEmpty() && !supports(Capability.PARTITION)) {
            warnings.add("方言 " + name + " 不支持分区，分区列已降级为普通列");
        }
        if (!table.primaryKeyColumns().isEmpty() && !supports(Capability.PRIMARY_KEY)) {
            warnings.add("方言 " + name + " 不支持表级主键，已忽略主键定义");
        }
        if (hasText(table.getTableOptions()) && !supports(Capability.TABLE_OPTIONS)) {
            warnings.add("方言 " + name + " 不支持表选项，已忽略: " + table.getTableOptions());
        }
        int commentedColumns = 0;
        for (ColumnMeta c : table.getColumns()) {
            if (!supportsType(c.getLogicalType())) {
                throw new BizException(400, "方言 " + name + " 不支持逻辑类型 "
                        + c.getLogicalType() + "（列 " + c.getColumnName() + "）");
            }
            if (hasText(c.getComment())) {
                commentedColumns++;
            }
        }
        if (hasText(table.getComment())
                && !supports(Capability.INLINE_TABLE_COMMENT)
                && !supports(Capability.POST_DDL_COMMENT)) {
            warnings.add("方言 " + name + " 不支持表注释，已忽略: " + table.getComment());
        }
        if (commentedColumns > 0
                && !supports(Capability.INLINE_COLUMN_COMMENT)
                && !supports(Capability.POST_DDL_COMMENT)) {
            warnings.add("方言 " + name + " 不支持列注释，已忽略 " + commentedColumns + " 处");
        }
    }

    private static String normalizeType(String s) {
        return s.trim().toUpperCase(java.util.Locale.ROOT);
    }

    protected static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    protected static void addIfText(List<String> list, String value) {
        if (hasText(value)) {
            list.add(value.trim());
        }
    }

    protected static String withLength(String base, ColumnMeta c) {
        return c.getLength() != null && c.getLength() > 0 ? base + "(" + c.getLength() + ")" : base;
    }

    protected static String withPrecision(String base, ColumnMeta c) {
        Integer p = c.getPrecision();
        if (p == null || p <= 0) {
            return base;
        }
        Integer s = c.getScale();
        return s == null ? base + "(" + p + ")" : base + "(" + p + "," + s + ")";
    }

    /** 单引号转义，防止注释里的 ' 破坏 DDL */
    protected static String literal(String s) {
        return s == null ? "" : s.replace("'", "''");
    }
}
