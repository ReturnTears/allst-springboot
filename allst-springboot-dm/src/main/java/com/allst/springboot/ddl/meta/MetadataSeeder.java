package com.allst.springboot.ddl.meta;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static com.allst.springboot.ddl.meta.LogicalTypes.*;

/**
 * 示例元数据种子。默认不在启动时执行，由 /api/dm/ddl/seed 手动触发，
 * 免得一套「演示数据」污染真实库。测试里显式调用它准备数据。
 *
 * <p>覆盖五种差异明显的目标：MySQL（自增+内联注释）、PostgreSQL（SERIAL+COMMENT ON）、
 * Oracle（大写标识符+SEQUENCE）、Impala（外部表+分区+PARQUET）、ClickHouse（MergeTree+分区）。
 */
@Component
public class MetadataSeeder {

    private final TableMetaRepository repository;

    public MetadataSeeder(TableMetaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int seed() {
        repository.deleteAll();
        List<TableMeta> samples = Arrays.asList(mysqlUser(), pgUser(), oracleEmployee(), impalaOrder(), clickhouseEvent());
        repository.saveAll(samples);
        return samples.size();
    }

    private TableMeta mysqlUser() {
        return TableMeta.builder()
                .schemaName("allst_dm")
                .tableName("sys_user")
                .dialect("MYSQL")
                .comment("系统用户表")
                .tableOptions("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci")
                .column(ColumnMeta.builder().columnName("id").logicalType(BIGINT).primaryKey(true)
                        .autoIncrement(true).comment("主键").ordinal(0).build())
                .column(ColumnMeta.builder().columnName("user_name").logicalType(STRING).length(64)
                        .nullable(false).comment("用户名").ordinal(1).build())
                .column(ColumnMeta.builder().columnName("age").logicalType(INT).ordinal(2).build())
                .column(ColumnMeta.builder().columnName("balance").logicalType(DECIMAL)
                        .precision(12).scale(2).defaultValue("0").ordinal(3).build())
                .column(ColumnMeta.builder().columnName("active").logicalType(BOOL)
                        .defaultValue("1").ordinal(4).build())
                .column(ColumnMeta.builder().columnName("created_at").logicalType(DATETIME).ordinal(5).build())
                .build();
    }

    private TableMeta pgUser() {
        return TableMeta.builder()
                .schemaName("public")
                .tableName("sys_user")
                .dialect("POSTGRESQL")
                .comment("系统用户表")
                .column(ColumnMeta.builder().columnName("id").logicalType(BIGINT).primaryKey(true)
                        .autoIncrement(true).ordinal(0).build())
                .column(ColumnMeta.builder().columnName("email").logicalType(STRING).length(128)
                        .nullable(false).ordinal(1).build())
                .column(ColumnMeta.builder().columnName("profile").logicalType(JSON).ordinal(2).build())
                .column(ColumnMeta.builder().columnName("created_at").logicalType(TIMESTAMP).ordinal(3).build())
                .build();
    }

    private TableMeta oracleEmployee() {
        return TableMeta.builder()
                .schemaName("HR")
                .tableName("EMPLOYEE")
                .dialect("ORACLE")
                .comment("员工表")
                .tableOptions("TABLESPACE USERS")
                .column(ColumnMeta.builder().columnName("ID").logicalType(BIGINT).primaryKey(true)
                        .autoIncrement(true).ordinal(0).build())
                .column(ColumnMeta.builder().columnName("NAME").logicalType(STRING).length(64)
                        .nullable(false).ordinal(1).build())
                .column(ColumnMeta.builder().columnName("SALARY").logicalType(DECIMAL)
                        .precision(10).scale(2).ordinal(2).build())
                .column(ColumnMeta.builder().columnName("HIRE_DATE").logicalType(DATETIME).ordinal(3).build())
                .build();
    }

    private TableMeta impalaOrder() {
        return TableMeta.builder()
                .schemaName("dwd")
                .tableName("dwd_order")
                .dialect("IMPALA")
                .comment("订单明细事实表")
                .externalTable(true)
                .tableOptions("STORED AS PARQUET")
                .column(ColumnMeta.builder().columnName("order_id").logicalType(STRING).ordinal(0).build())
                .column(ColumnMeta.builder().columnName("sku").logicalType(STRING).ordinal(1).build())
                .column(ColumnMeta.builder().columnName("amount").logicalType(DECIMAL)
                        .precision(12).scale(2).ordinal(2).build())
                .column(ColumnMeta.builder().columnName("dt").logicalType(STRING)
                        .partition(true).ordinal(3).build())
                .build();
    }

    private TableMeta clickhouseEvent() {
        return TableMeta.builder()
                .schemaName("analytics")
                .tableName("event_log")
                .dialect("CLICKHOUSE")
                .comment("埋点事件日志")
                .tableOptions("ENGINE=MergeTree() ORDER BY (id, event_name)")
                .column(ColumnMeta.builder().columnName("id").logicalType(BIGINT).ordinal(0).build())
                .column(ColumnMeta.builder().columnName("event_name").logicalType(STRING).ordinal(1).build())
                .column(ColumnMeta.builder().columnName("ts").logicalType(DATETIME).ordinal(2).build())
                .column(ColumnMeta.builder().columnName("dt").logicalType(DATE)
                        .partition(true).ordinal(3).build())
                .build();
    }
}
