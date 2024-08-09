package com.allst.boot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:39
 */
@Data
@AllArgsConstructor
public class Column {
    public int inx;
    public String colName; // 列名
    public String dataType; // 类型
    public String schema; // 数据库
    public String table; // 表
}
