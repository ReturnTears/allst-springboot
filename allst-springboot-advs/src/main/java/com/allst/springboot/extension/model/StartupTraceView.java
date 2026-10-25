package com.allst.springboot.extension.model;

/**
 * 把真实执行记录和该阶段的使用场景拼在一起，方便对照回溯。
 * 访问方法手写，避免 IDE 未启用 Lombok 注解处理时找不到 getSequence。
 */
public class StartupTraceView {

    private final int sequence;
    private final int phaseOrder;
    private final String phase;
    private final String extensionPoint;
    private final String action;
    private final String scenario;
    private final String implementation;
    private final long timestampMillis;

    public StartupTraceView(int sequence, int phaseOrder, String phase, String extensionPoint,
                            String action, String scenario, String implementation, long timestampMillis) {
        this.sequence = sequence;
        this.phaseOrder = phaseOrder;
        this.phase = phase;
        this.extensionPoint = extensionPoint;
        this.action = action;
        this.scenario = scenario;
        this.implementation = implementation;
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

    public String getScenario() {
        return scenario;
    }

    public String getImplementation() {
        return implementation;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
