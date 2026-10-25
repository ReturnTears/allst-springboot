package com.allst.springboot.extension.model;

/**
 * 一次启动里某个扩展点实际执行后留下的记录。
 */
public class StartupRecord {

    private final int sequence;
    private final int phaseOrder;
    private final String phase;
    private final String extensionPoint;
    private final String action;
    private final long timestampMillis;

    public StartupRecord(int sequence, int phaseOrder, String phase, String extensionPoint,
                         String action, long timestampMillis) {
        this.sequence = sequence;
        this.phaseOrder = phaseOrder;
        this.phase = phase;
        this.extensionPoint = extensionPoint;
        this.action = action;
        this.timestampMillis = timestampMillis;
    }

    public int getSequence() {
        return sequence;
    }

    public int getPhaseOrder() {
        return phaseOrder;
    }

    public String getPhase() {
        return phase;
    }

    public String getExtensionPoint() {
        return extensionPoint;
    }

    public String getAction() {
        return action;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
