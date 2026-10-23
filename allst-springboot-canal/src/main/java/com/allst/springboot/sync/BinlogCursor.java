package com.allst.springboot.sync;

public final class BinlogCursor {

    public static final String FILE = "_binlogFile";
    public static final String OFFSET = "_binlogOffset";
    public static final String EVENT_TYPE = "_eventType";
    public static final String SCHEMA = "_schema";
    public static final String TABLE = "_table";

    private BinlogCursor() {
    }

    /**
     * MySQL binlog 文件名默认零填充，同一文件内再用 offset 比较。
     * 相同位点允许再次写入，用来消化 Canal 和 MQ 的重复投递。
     */
    public static boolean shouldApply(String storedFile, Long storedOffset, String incomingFile, long incomingOffset) {
        if (storedFile == null || storedFile.isEmpty() || storedOffset == null) {
            return true;
        }
        if (incomingFile == null || incomingFile.isEmpty()) {
            return true;
        }
        int compared = incomingFile.compareTo(storedFile);
        if (compared > 0) {
            return true;
        }
        if (compared < 0) {
            return false;
        }
        return incomingOffset >= storedOffset;
    }
}
