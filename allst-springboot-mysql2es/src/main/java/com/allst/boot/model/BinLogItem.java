package com.allst.boot.model;

import com.github.shyiko.mysql.binlog.event.EventType;
import com.google.common.collect.Maps;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

import static com.github.shyiko.mysql.binlog.event.EventType.isDelete;
import static com.github.shyiko.mysql.binlog.event.EventType.isWrite;

/**
 * @author Hutu
 * @since 2024-08-09 下午 10:41
 */
@Data
public class BinLogItem implements Serializable {
    private static final long serialVersionUID = 5503152746318421290L;
    private String dbTable;
    private EventType eventType;
    private Long timestamp = null;
    private Long serverId = null;
    // 存储字段-之前的值之后的值
    private Map<String, Serializable> before = null;
    private Map<String, Serializable> after = null;
    // 存储字段--类型
    private Map<String, Column> colums = null;

    /**
     * 新增或者删除操作数据格式化
     */
    public static BinLogItem itemFromInsertOrDeleted(Serializable[] row, Map<String, Column> columMap, EventType eventType) {
        if (null == row || null == columMap) {
            return null;
        }
        if (row.length != columMap.size()) {
            return null;
        }
        // 初始化Item
        BinLogItem item = new BinLogItem();
        item.eventType = eventType;
        item.colums = columMap;
        item.before = Maps.newHashMap();
        item.after = Maps.newHashMap();

        Map<String, Serializable> beOrAf = Maps.newHashMap();

        columMap.forEach((key, colum) -> beOrAf.put(key, row[colum.inx]));

        // 写操作放after，删操作放before
        if (isWrite(eventType)) {
            item.after = beOrAf;
        }
        if (isDelete(eventType)) {
            item.before = beOrAf;
        }

        return item;
    }

    /**
     * 更新操作数据格式化
     */
    public static BinLogItem itemFromUpdate(Map.Entry<Serializable[], Serializable[]> mapEntry, Map<String, Column> columMap, EventType eventType) {
        if (null == mapEntry || null == columMap) {
            return null;
        }
        // 初始化Item
        BinLogItem item = new BinLogItem();
        item.eventType = eventType;
        item.colums = columMap;
        item.before = Maps.newHashMap();
        item.after = Maps.newHashMap();

        Map<String, Serializable> be = Maps.newHashMap();
        Map<String, Serializable> af = Maps.newHashMap();

        columMap.forEach((key, colum) -> {
            be.put(key, mapEntry.getKey()[colum.inx]);
            af.put(key, mapEntry.getValue()[colum.inx]);
        });

        item.before = be;
        item.after = af;
        return item;
    }
}
