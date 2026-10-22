package com.allst.springboot.redis.durable;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 只读取持久化状态，不修改 redis.conf，也不执行 BGSAVE。
 * RDB 是某个时间点的快照，AOF 是写命令日志。两者可以同时打开。
 * 对应文档 docs/17-durable.md。
 */
@Service
public class PersistenceService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final StringRedisTemplate redis;

    public PersistenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> info() {
        Properties server = info("server");
        Properties persistence = info("persistence");
        Properties memory = info("memory");
        Properties appendonly = config("appendonly");
        Properties save = config("save");

        List<Map<String, String>> fields = new ArrayList<>();
        fields.add(field("redis_version", server.getProperty("redis_version"), "服务端版本"));
        fields.add(field("redis_mode", server.getProperty("redis_mode"), "standalone 表示单机，本示例按单机来写"));
        fields.add(field("used_memory_human", memory.getProperty("used_memory_human"), "当前内存占用"));
        fields.add(field("aof_enabled", persistence.getProperty("aof_enabled"), "1 表示正在使用 AOF"));
        fields.add(field("appendonly", appendonly.getProperty("appendonly"), "配置项：yes 会在重启后按命令日志恢复"));
        fields.add(field("aof_last_write_status", persistence.getProperty("aof_last_write_status"), "最近一次 AOF 写入是否成功"));
        fields.add(field("rdb_last_bgsave_status", persistence.getProperty("rdb_last_bgsave_status"), "最近一次 RDB 后台保存是否成功"));
        fields.add(field("rdb_last_save_time", formatEpoch(persistence.getProperty("rdb_last_save_time")), "最近一次成功 RDB 的时间"));
        fields.add(field("rdb_changes_since_last_save", persistence.getProperty("rdb_changes_since_last_save"), "距离上次快照又写了多少次"));
        fields.add(field("save", save.getProperty("save"), "RDB 自动快照规则，例如 3600 秒内至少 1 次写入"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fields", fields);
        body.put("note", "内存是工作区，RDB/AOF 是恢复依据。关停机器前至少要有一种持久化，否则重启后数据会丢。");
        return body;
    }

    private Properties info(String section) {
        Properties properties = redis.execute((RedisCallback<Properties>) connection -> connection.info(section));
        return properties == null ? new Properties() : properties;
    }

    private Properties config(String pattern) {
        Properties properties = redis.execute((RedisCallback<Properties>) connection -> connection.getConfig(pattern));
        return properties == null ? new Properties() : properties;
    }

    private String formatEpoch(String epochSeconds) {
        if (epochSeconds == null || epochSeconds.isBlank() || "0".equals(epochSeconds)) {
            return "还没有成功的 RDB";
        }
        try {
            return TIME.format(Instant.ofEpochSecond(Long.parseLong(epochSeconds)));
        } catch (NumberFormatException ex) {
            return epochSeconds;
        }
    }

    private Map<String, String> field(String name, String value, String meaning) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("value", value == null ? "" : value);
        row.put("meaning", meaning);
        return row;
    }
}
