package com.allst.springboot.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.allst.springboot.model.DataChangeEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanalEntryParserTest {

    private final CanalEntryParser parser = new CanalEntryParser();

    @Test
    void parsesInsertAndKeepsPrimaryKey() {
        CanalEntry.Entry entry = row(CanalEntry.EventType.INSERT, List.of(), List.of(
                column("id", "1001", true, false),
                column("name", "Canal入门", false, false),
                column("stock", null, false, true)));

        List<DataChangeEvent> events = parser.parse(List.of(entry));

        assertEquals(1, events.size());
        DataChangeEvent event = events.get(0);
        assertEquals(DataChangeEvent.INSERT, event.getEventType());
        assertEquals("demo", event.getSchema());
        assertEquals("t_product", event.getTable());
        assertEquals(List.of("id"), event.getPrimaryKeys());
        assertEquals("1001", event.getPrimaryValue());
        assertEquals("Canal入门", event.getAfter().get("name"));
        assertEquals("", event.getAfter().get("stock"));
        assertEquals("mysql-bin.000001", event.getLogfile());
        assertEquals(120L, event.getLogfileOffset());
    }

    @Test
    void deleteUsesBeforeImage() {
        CanalEntry.Entry entry = row(CanalEntry.EventType.DELETE, List.of(
                column("id", "1001", true, false),
                column("name", "Canal入门", false, false)), List.of());

        DataChangeEvent event = parser.parse(List.of(entry)).get(0);

        assertEquals(DataChangeEvent.DELETE, event.getEventType());
        assertEquals("1001", event.getPrimaryValue());
        assertEquals("Canal入门", event.getBefore().get("name"));
        assertTrue(event.getAfter().isEmpty());
    }

    @Test
    void joinsCompositePrimaryKey() {
        CanalEntry.Entry entry = row(CanalEntry.EventType.UPDATE, List.of(
                column("sku_id", "8", true, false),
                column("wh_id", "2", true, false)), List.of(
                column("sku_id", "8", true, false),
                column("wh_id", "2", true, false),
                column("stock", "7", false, false)));

        DataChangeEvent event = parser.parse(List.of(entry)).get(0);

        assertEquals(List.of("sku_id", "wh_id"), event.getPrimaryKeys());
        assertEquals("8:2", event.getPrimaryValue());
        assertEquals("7", event.getAfter().get("stock"));
    }

    @Test
    void skipsTransactionAndDdlAndTablesWithoutPrimaryKey() {
        CanalEntry.Entry begin = CanalEntry.Entry.newBuilder()
                .setHeader(header())
                .setEntryType(CanalEntry.EntryType.TRANSACTIONBEGIN)
                .build();
        CanalEntry.RowChange ddl = CanalEntry.RowChange.newBuilder()
                .setIsDdl(true)
                .setEventType(CanalEntry.EventType.CREATE)
                .setSql("create table t_product (id bigint primary key)")
                .build();
        CanalEntry.Entry ddlEntry = CanalEntry.Entry.newBuilder()
                .setHeader(header())
                .setEntryType(CanalEntry.EntryType.ROWDATA)
                .setStoreValue(ddl.toByteString())
                .build();
        CanalEntry.Entry noKey = row(CanalEntry.EventType.INSERT, List.of(), List.of(column("name", "x", false, false)));

        assertTrue(parser.parse(List.of(begin, ddlEntry, noKey)).isEmpty());
    }

    private CanalEntry.Entry row(CanalEntry.EventType eventType, List<CanalEntry.Column> before, List<CanalEntry.Column> after) {
        CanalEntry.RowChange change = CanalEntry.RowChange.newBuilder()
                .setEventType(eventType)
                .setIsDdl(false)
                .addRowDatas(CanalEntry.RowData.newBuilder()
                        .addAllBeforeColumns(before)
                        .addAllAfterColumns(after)
                        .build())
                .build();
        return CanalEntry.Entry.newBuilder()
                .setHeader(header())
                .setEntryType(CanalEntry.EntryType.ROWDATA)
                .setStoreValue(change.toByteString())
                .build();
    }

    private CanalEntry.Header header() {
        return CanalEntry.Header.newBuilder()
                .setSchemaName("demo")
                .setTableName("t_product")
                .setLogfileName("mysql-bin.000001")
                .setLogfileOffset(120L)
                .setExecuteTime(1_700_000_000_000L)
                .build();
    }

    private CanalEntry.Column column(String name, String value, boolean key, boolean nullValue) {
        CanalEntry.Column.Builder builder = CanalEntry.Column.newBuilder()
                .setName(name)
                .setIsKey(key)
                .setUpdated(true)
                .setIsNull(nullValue);
        if (value != null) {
            builder.setValue(value);
        }
        return builder.build();
    }
}
