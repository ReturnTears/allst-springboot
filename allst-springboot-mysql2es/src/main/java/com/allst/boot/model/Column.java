package com.allst.boot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:39
 */
@Data
public class Column {
    public int inx;
    public String colName; // 列名
    public String dataType; // 类型
    public String schema; // 数据库
    public String table; // 表

    public Column(String colName, String dataType, int inx, String schema, String table) {
        this.colName = colName;
        this.dataType = dataType;
        this.inx = inx;
        this.schema = schema;
        this.table = table;
    }
}
