package com.allst.springboot.ddl.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 表元数据。一张目标表 + 它的列集合 + 方言标识。
 *
 * <p>这是「建造者」模式的主战场：字段多、约束多（表名/方言必填、列名不可重复、
 * ordinal 不可重复），用 build() 一次性校验，产出可安全交给生成器的对象。
 */
@Entity
@Table(name = "dm_table_meta")
@Getter
@Setter
@NoArgsConstructor
public class TableMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", length = 128)
    private String schemaName;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    /** 方言标识，如 MYSQL / POSTGRESQL / IMPALA，对应一个 DdlDialect bean */
    @Column(name = "dialect", nullable = false, length = 32)
    private String dialect;

    @Column(name = "table_comment", length = 512)
    private String comment;

    /** Impala / Hive 的外部表；影响 CREATE 前缀 */
    @Column(name = "external_table")
    private boolean externalTable;

    /** 方言专有表选项原文，如 "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" */
    @Column(name = "table_options", length = 1024)
    private String tableOptions;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ordinal ASC")
    private List<ColumnMeta> columns = new ArrayList<>();

    public static Builder builder() {
        return new Builder();
    }

    public void addColumn(ColumnMeta c) {
        c.setTable(this);
        columns.add(c);
    }

    public List<ColumnMeta> primaryKeyColumns() {
        List<ColumnMeta> pk = new ArrayList<>();
        for (ColumnMeta c : columns) {
            if (c.isPrimaryKey()) {
                pk.add(c);
            }
        }
        pk.sort((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()));
        return pk;
    }

    public List<ColumnMeta> partitionColumns() {
        List<ColumnMeta> parts = new ArrayList<>();
        for (ColumnMeta c : columns) {
            if (c.isPartition()) {
                parts.add(c);
            }
        }
        parts.sort((a, b) -> Integer.compare(a.getOrdinal(), b.getOrdinal()));
        return parts;
    }

    public static final class Builder {
        private final TableMeta t = new TableMeta();
        private final List<ColumnMeta> columns = new ArrayList<>();

        public Builder schemaName(String v) {
            t.schemaName = v;
            return this;
        }

        public Builder tableName(String v) {
            t.tableName = v;
            return this;
        }

        public Builder dialect(String v) {
            t.dialect = v;
            return this;
        }

        public Builder comment(String v) {
            t.comment = v;
            return this;
        }

        public Builder externalTable(boolean v) {
            t.externalTable = v;
            return this;
        }

        public Builder tableOptions(String v) {
            t.tableOptions = v;
            return this;
        }

        public Builder column(ColumnMeta c) {
            columns.add(c);
            return this;
        }

        public Builder columns(ColumnMeta... cs) {
            for (ColumnMeta c : cs) {
                columns.add(c);
            }
            return this;
        }

        public TableMeta build() {
            if (t.tableName == null || t.tableName.isBlank()) {
                throw new IllegalArgumentException("表名不能为空");
            }
            if (t.dialect == null || t.dialect.isBlank()) {
                throw new IllegalArgumentException("方言不能为空: " + t.tableName);
            }
            Set<String> names = new HashSet<>();
            Set<Integer> ordinals = new HashSet<>();
            for (ColumnMeta c : columns) {
                if (!names.add(c.getColumnName())) {
                    throw new IllegalArgumentException("列名重复: " + c.getColumnName());
                }
                if (!ordinals.add(c.getOrdinal())) {
                    throw new IllegalArgumentException("ordinal 重复: " + c.getOrdinal());
                }
                t.addColumn(c);
            }
            return t;
        }
    }
}
