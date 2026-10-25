package com.allst.springboot.extension.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 把真实执行记录和该阶段的使用场景拼在一起，方便对照回溯。
 */
@Getter
@AllArgsConstructor
public class StartupTraceView {

    private final int sequence;
    private final int phaseOrder;
    private final String phase;
    private final String extensionPoint;
    private final String action;
    private final String scenario;
    private final String implementation;
    private final long timestampMillis;
}
