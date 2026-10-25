package com.allst.springboot.extension.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 启动扩展点的回溯入口摘要。
 */
@Getter
@Builder
public class ExtensionSummary {

    private final String springBootVersion;
    private final String banner;
    private final boolean cacheWarmup;
    private final String dynamicNotice;
    private final String importedModule;
    private final long startedAtMillis;
    private final long elapsedMillis;
    private final int traceSize;
    private final int catalogSize;
    private final int cachePointCount;
    private final List<String> revisit;
    private final List<String> latestActions;
}
