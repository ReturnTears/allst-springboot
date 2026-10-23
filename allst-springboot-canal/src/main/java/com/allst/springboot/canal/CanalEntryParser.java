package com.allst.springboot.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.allst.springboot.model.DataChangeEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 Canal TCP 协议里的 Entry 转成与中间件无关的 {@link DataChangeEvent}。
 * 参考 alibaba/canal 的 SimpleCanalClientExample：只处理 ROWDATA 里的 INSERT / UPDATE / DELETE。
 */
@Slf4j
@Component
public class CanalEntryParser {

    public List<DataChangeEvent> parse(List<CanalEntry.Entry> entries) {
        List<DataChangeEvent> events = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return events;
        }
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            CanalEntry.RowChange rowChange = readRowChange(entry);
            if (rowChange == null || rowChange.getIsDdl()) {
                continue;
            }
            CanalEntry.EventType eventType = rowChange.getEventType();
            if (!isDml(eventType)) {
                continue;
            }
            String schema = entry.getHeader().getSchemaName();
            String table = entry.getHeader().getTableName();
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                DataChangeEvent event = toEvent(entry, eventType, schema, table, rowData);
                if (event != null) {
                    events.add(event);
                }
            }
        }
        return events;
    }

    private CanalEntry.RowChange readRowChange(CanalEntry.Entry entry) {
        try {
            return CanalEntry.RowChange.parseFrom(entry.getStoreValue());
        } catch (InvalidProtocolBufferException ex) {
            CanalEntry.Header header = entry.getHeader();
            log.error("跳过无法解析的行变更, schema={}, table={}, logfile={}, offset={}",
                    header.getSchemaName(), header.getTableName(), header.getLogfileName(), header.getLogfileOffset(), ex);
            return null;
        }
    }

    private DataChangeEvent toEvent(CanalEntry.Entry entry, CanalEntry.EventType eventType, String schema, String table,
                                    CanalEntry.RowData rowData) {
        List<CanalEntry.Column> beforeColumns = rowData.getBeforeColumnsList();
        List<CanalEntry.Column> afterColumns = rowData.getAfterColumnsList();
        List<CanalEntry.Column> keySource = hasKey(afterColumns) ? afterColumns : beforeColumns;
        if (!hasKey(keySource)) {
            log.warn("跳过没有主键的变更, schema={}, table={}, event={}", schema, table, eventType);
            return null;
        }
        CanalEntry.Header header = entry.getHeader();
        DataChangeEvent event = new DataChangeEvent();
        event.setEventType(eventType.name());
        event.setSchema(schema);
        event.setTable(table);
        event.setExecuteTime(header.getExecuteTime());
        event.setLogfile(header.getLogfileName());
        event.setLogfileOffset(header.getLogfileOffset());
        event.setPrimaryKeys(keyNames(keySource));
        event.setPrimaryValue(keyValue(keySource));
        event.setBefore(toMap(beforeColumns));
        event.setAfter(toMap(afterColumns));
        event.setMessageId(schema + ":" + table + ":" + event.getPrimaryValue() + ":"
                + header.getLogfileName() + ":" + header.getLogfileOffset() + ":" + eventType.name());
        return event;
    }

    private boolean isDml(CanalEntry.EventType eventType) {
        return eventType == CanalEntry.EventType.INSERT
                || eventType == CanalEntry.EventType.UPDATE
                || eventType == CanalEntry.EventType.DELETE;
    }

    private boolean hasKey(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            if (column.getIsKey()) {
                return true;
            }
        }
        return false;
    }

    private List<String> keyNames(List<CanalEntry.Column> columns) {
        List<String> names = new ArrayList<>();
        for (CanalEntry.Column column : columns) {
            if (column.getIsKey()) {
                names.add(column.getName());
            }
        }
        return names;
    }

    private String keyValue(List<CanalEntry.Column> columns) {
        StringBuilder value = new StringBuilder();
        for (CanalEntry.Column column : columns) {
            if (!column.getIsKey()) {
                continue;
            }
            if (value.length() > 0) {
                value.append(':');
            }
            value.append(column.getValue());
        }
        return value.toString();
    }

    private Map<String, String> toMap(List<CanalEntry.Column> columns) {
        Map<String, String> values = new LinkedHashMap<>();
        for (CanalEntry.Column column : columns) {
            values.put(column.getName(), column.getIsNull() ? "" : column.getValue());
        }
        return values;
    }
}
