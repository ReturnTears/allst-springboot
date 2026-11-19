package com.allst.springboot.ddl.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 字段元数据。一行描述目标库里的一列。
 *
 * <p>设计要点：
 * <ul>
 *   <li>logicalType 存逻辑类型名（见 {@link LogicalTypes}），物理类型由方言渲染；</li>
 *   <li>partition 标记这一列是分区列（Impala/Hive/ClickHouse/Doris 用），Stream 渲染时会被分流；</li>
 *   <li>ordinal 决定列顺序，必须显式且唯一，否则 DDL 输出抖动、版本库 diff 难看。</li>
 * </ul>
 * 建造者是本类推荐的构造入口：build() 里做校验并把主键强制 NOT NULL、自增强制主键。
 * setter 主要留给 JPA 与反序列化使用。
 */
@Entity
@Table(name = "dm_column_meta")
@Getter
@Setter
@NoArgsConstructor
public class ColumnMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = javax.persistence.FetchType.LAZY)
    @JoinColumn(name = "table_meta_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private TableMeta table;

    @Column(name = "column_name", nullable = false, length = 128)
    private String columnName;

    @Column(name = "logical_type", nullable = false, length = 64)
    private String logicalType;

    @Column(name = "col_length")
    private Integer length;

    @Column(name = "col_precision")
    private Integer precision;

    @Column(name = "col_scale")
    private Integer scale;

    @Column(name = "is_nullable")
    private boolean nullable = true;

    @Column(name = "is_primary_key")
    private boolean primaryKey;

    @Column(name = "is_auto_increment")
    private boolean autoIncrement;

    @Column(name = "is_partition")
    private boolean partition;

    @Column(name = "default_value", length = 256)
    private String defaultValue;

    @Column(name = "col_comment", length = 512)
    private String comment;

    @Column(name = "ordinal")
    private int ordinal;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final ColumnMeta c = new ColumnMeta();

        public Builder columnName(String v) {
            c.columnName = v;
            return this;
        }

        public Builder logicalType(String v) {
            c.logicalType = v;
            return this;
        }

        public Builder length(Integer v) {
            c.length = v;
            return this;
        }

        public Builder precision(Integer v) {
            c.precision = v;
            return this;
        }

        public Builder scale(Integer v) {
            c.scale = v;
            return this;
        }

        public Builder nullable(boolean v) {
            c.nullable = v;
            return this;
        }

        public Builder primaryKey(boolean v) {
            c.primaryKey = v;
            return this;
        }

        public Builder autoIncrement(boolean v) {
            c.autoIncrement = v;
            return this;
        }

        public Builder partition(boolean v) {
            c.partition = v;
            return this;
        }

        public Builder defaultValue(String v) {
            c.defaultValue = v;
            return this;
        }

        public Builder comment(String v) {
            c.comment = v;
            return this;
        }

        public Builder ordinal(int v) {
            c.ordinal = v;
            return this;
        }

        public ColumnMeta build() {
            if (c.columnName == null || c.columnName.isBlank()) {
                throw new IllegalArgumentException("列名不能为空");
            }
            if (c.logicalType == null || c.logicalType.isBlank()) {
                throw new IllegalArgumentException("逻辑类型不能为空: " + c.columnName);
            }
            if (c.ordinal < 0) {
                throw new IllegalArgumentException("ordinal 不能为负: " + c.columnName);
            }
            if (c.autoIncrement && !c.primaryKey) {
                throw new IllegalArgumentException("自增列必须是主键: " + c.columnName);
            }
            if (c.primaryKey) {
                c.nullable = false;
            }
            return c;
        }
    }
}
