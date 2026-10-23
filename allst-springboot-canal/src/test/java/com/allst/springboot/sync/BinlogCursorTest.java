package com.allst.springboot.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinlogCursorTest {

    @Test
    void appliesWhenDestinationHasNoCursor() {
        assertTrue(BinlogCursor.shouldApply(null, null, "mysql-bin.000001", 10));
        assertTrue(BinlogCursor.shouldApply("", null, "mysql-bin.000001", 10));
    }

    @Test
    void comparesFileNameThenOffset() {
        assertTrue(BinlogCursor.shouldApply("mysql-bin.000001", 100L, "mysql-bin.000001", 120L));
        assertTrue(BinlogCursor.shouldApply("mysql-bin.000001", 100L, "mysql-bin.000001", 100L));
        assertFalse(BinlogCursor.shouldApply("mysql-bin.000001", 100L, "mysql-bin.000001", 99L));
        assertTrue(BinlogCursor.shouldApply("mysql-bin.000001", 999L, "mysql-bin.000002", 1L));
        assertFalse(BinlogCursor.shouldApply("mysql-bin.000002", 1L, "mysql-bin.000001", 999L));
    }
}
