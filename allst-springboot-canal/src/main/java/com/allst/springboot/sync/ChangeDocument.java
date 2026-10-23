package com.allst.springboot.sync;

import com.allst.springboot.model.DataChangeEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ChangeDocument {

    private ChangeDocument() {
    }

    public static String targetName(String schema, String table) {
        return (schema + "_" + table).toLowerCase(Locale.ROOT);
    }

    public static String redisKey(String prefix, String schema, String table, String id) {
        return prefix + ":" + schema + ":" + table + ":" + id;
    }

    public static Map<String, Object> row(DataChangeEvent event) {
        Map<String, Object> document = new LinkedHashMap<>();
        if (event.getAfter() != null) {
            document.putAll(event.getAfter());
        }
        document.put(BinlogCursor.FILE, event.getLogfile());
        document.put(BinlogCursor.OFFSET, event.getLogfileOffset());
        document.put(BinlogCursor.EVENT_TYPE, event.getEventType());
        document.put(BinlogCursor.SCHEMA, event.getSchema());
        document.put(BinlogCursor.TABLE, event.getTable());
        return document;
    }

    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }
}
