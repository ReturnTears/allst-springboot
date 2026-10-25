package com.allst.springboot.extension.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 一次启动里某个扩展点实际执行后留下的记录。
 */
@Getter
@AllArgsConstructor
public class StartupRecord {

    private final int sequence;
    private final int phaseOrder;
    private final String phase;
    private final String extensionPoint;
    private final String action;
    private final long timestampMillis;
}
