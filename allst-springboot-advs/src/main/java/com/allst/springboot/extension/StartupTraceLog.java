package com.allst.springboot.extension;

import com.allst.springboot.extension.model.CachePointDoc;
import com.allst.springboot.extension.model.StartupRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动轨迹。
 * <p>
 * 用静态容器，是因为 {@code SpringApplicationRunListener}、{@code EnvironmentPostProcessor}
 * 执行时 ApplicationContext 还不存在，不能注入 Bean。
 */
public final class StartupTraceLog {

    private static final Logger log = LoggerFactory.getLogger(StartupTraceLog.class);
    private static final List<StartupRecord> RECORDS = Collections.synchronizedList(new ArrayList<>());
    private static final List<CachePointDoc> CACHE_POINTS = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private StartupTraceLog() {
    }

    public static void record(StartupPhase phase, String action) {
        StartupRecord record;
        synchronized (RECORDS) {
            record = new StartupRecord(
                    SEQUENCE.incrementAndGet(),
                    phase.getOrder(),
                    phase.getTitle(),
                    phase.getExtensionPoint(),
                    action,
                    System.currentTimeMillis());
            RECORDS.add(record);
        }
        log.info("[启动扩展点][#{}][phase={}] {} | {}",
                record.getSequence(), record.getPhaseOrder(), record.getExtensionPoint(), action);
    }

    public static void addCachePoint(CachePointDoc doc) {
        String key = doc.getBeanName() + "#" + doc.getMethodName() + "#" + doc.getAnnotation() + "#" + doc.getKey();
        synchronized (CACHE_POINTS) {
            for (CachePointDoc existing : CACHE_POINTS) {
                String existingKey = existing.getBeanName() + "#" + existing.getMethodName()
                        + "#" + existing.getAnnotation() + "#" + existing.getKey();
                if (existingKey.equals(key)) {
                    return;
                }
            }
            CACHE_POINTS.add(doc);
        }
    }

    public static List<StartupRecord> snapshot() {
        synchronized (RECORDS) {
            return List.copyOf(RECORDS);
        }
    }

    public static List<CachePointDoc> cachePoints() {
        synchronized (CACHE_POINTS) {
            return List.copyOf(CACHE_POINTS);
        }
    }

    public static int size() {
        synchronized (RECORDS) {
            return RECORDS.size();
        }
    }
}
