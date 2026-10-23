package com.allst.springboot.sync.redis;

import com.allst.springboot.canal.SyncProperties;
import com.allst.springboot.model.DataChangeEvent;
import com.allst.springboot.sync.BinlogCursor;
import com.allst.springboot.sync.ChangeDocument;
import com.allst.springboot.sync.SyncAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "sync.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisSyncService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SyncProperties syncProperties;

    public RedisSyncService(StringRedisTemplate stringRedisTemplate, SyncProperties syncProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.syncProperties = syncProperties;
    }

    public void apply(DataChangeEvent event) {
        if (event.getPrimaryValue() == null || event.getPrimaryValue().isEmpty()) {
            log.warn("Redis 跳过没有主键的变更, table={}", event.getTable());
            return;
        }
        String key = key(event.getSchema(), event.getTable(), event.getPrimaryValue());
        try {
            String storedFile = stringRedisTemplate.<String, String>opsForHash().get(key, BinlogCursor.FILE);
            String storedOffset = stringRedisTemplate.<String, String>opsForHash().get(key, BinlogCursor.OFFSET);
            if (!BinlogCursor.shouldApply(storedFile, ChangeDocument.toLong(storedOffset), event.getLogfile(), event.getLogfileOffset())) {
                log.info("Redis 跳过更旧的变更, key={}, incoming={}@{}", key, event.getLogfile(), event.getLogfileOffset());
                return;
            }
            if (event.delete()) {
                stringRedisTemplate.delete(key);
                log.info("Redis 已删除 {}", key);
                return;
            }
            Map<String, String> hash = new LinkedHashMap<>();
            ChangeDocument.row(event).forEach((name, value) -> hash.put(name, value == null ? "" : String.valueOf(value)));
            stringRedisTemplate.<String, String>opsForHash().putAll(key, hash);
            log.info("Redis 已写入 {}", key);
        } catch (RuntimeException ex) {
            throw new SyncAccessException("写入 Redis 失败: " + key, ex);
        }
    }

    public Map<String, String> find(String schema, String table, String id) {
        String key = key(schema, table, id);
        try {
            Map<String, String> entries = stringRedisTemplate.<String, String>opsForHash().entries(key);
            if (entries == null || entries.isEmpty()) {
                return null;
            }
            return new LinkedHashMap<>(entries);
        } catch (RuntimeException ex) {
            throw new SyncAccessException("读取 Redis 失败: " + key, ex);
        }
    }

    public String key(String schema, String table, String id) {
        return ChangeDocument.redisKey(syncProperties.getRedis().getKeyPrefix(), schema, table, id);
    }
}
