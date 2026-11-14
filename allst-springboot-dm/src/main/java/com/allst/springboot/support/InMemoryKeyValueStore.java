package com.allst.springboot.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认实现（dm.redis.enabled=false 或未配置）。带过期时间，语义与 Redis SETNX 对齐。
 */
@Component
@ConditionalOnProperty(name = "dm.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryKeyValueStore implements KeyValueStore {

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt.isBefore(Instant.now())) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    @Override
    public void put(String key, String value, long ttlSeconds) {
        entries.put(key, new Entry(value, Instant.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public boolean putIfAbsent(String key, String value, long ttlSeconds) {
        Entry fresh = new Entry(value, Instant.now().plusSeconds(ttlSeconds));
        Entry old = entries.putIfAbsent(key, fresh);
        if (old == null) {
            return true;
        }
        if (old.expireAt.isBefore(Instant.now())) {
            return entries.replace(key, old, fresh);
        }
        return false;
    }

    @Override
    public void delete(String key) {
        entries.remove(key);
    }

    private static final class Entry {
        private final String value;
        private final Instant expireAt;

        private Entry(String value, Instant expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
