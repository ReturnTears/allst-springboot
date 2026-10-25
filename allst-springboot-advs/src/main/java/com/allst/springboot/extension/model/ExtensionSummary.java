package com.allst.springboot.extension.model;

import java.util.List;

/**
 * 启动扩展点的回溯入口摘要。
 */
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

    public ExtensionSummary(String springBootVersion, String banner, boolean cacheWarmup, String dynamicNotice,
                            String importedModule, long startedAtMillis, long elapsedMillis, int traceSize,
                            int catalogSize, int cachePointCount, List<String> revisit, List<String> latestActions) {
        this.springBootVersion = springBootVersion;
        this.banner = banner;
        this.cacheWarmup = cacheWarmup;
        this.dynamicNotice = dynamicNotice;
        this.importedModule = importedModule;
        this.startedAtMillis = startedAtMillis;
        this.elapsedMillis = elapsedMillis;
        this.traceSize = traceSize;
        this.catalogSize = catalogSize;
        this.cachePointCount = cachePointCount;
        this.revisit = revisit;
        this.latestActions = latestActions;
    }

    public String getSpringBootVersion() {
        return springBootVersion;
    }

    public String getBanner() {
        return banner;
    }

    public boolean isCacheWarmup() {
        return cacheWarmup;
    }

    public String getDynamicNotice() {
        return dynamicNotice;
    }

    public String getImportedModule() {
        return importedModule;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public int getTraceSize() {
        return traceSize;
    }

    public int getCatalogSize() {
        return catalogSize;
    }

    public int getCachePointCount() {
        return cachePointCount;
    }

    public List<String> getRevisit() {
        return revisit;
    }

    public List<String> getLatestActions() {
        return latestActions;
    }
}
