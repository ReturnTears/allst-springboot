package com.allst.springboot.ddl.generation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一张表的生成结果。 */
@Getter
public final class TableDdl {

    private final String schemaName;
    private final String tableName;
    private final String sql;
    private final List<String> warnings;

    public TableDdl(String schemaName, String tableName, String sql, List<String> warnings) {
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.sql = sql;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }
}
