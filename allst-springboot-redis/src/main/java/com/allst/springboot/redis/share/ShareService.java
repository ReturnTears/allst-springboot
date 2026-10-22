package com.allst.springboot.redis.share;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.common.RedisValues;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 微服务之间不互相调用数据库，把共享配置和在线实例放在 Redis。
 * 配置用 Hash，改动时递增版本号，调用方可以据此判断要不要刷新本地副本。
 * 实例心跳是带 TTL 的字符串，进程停止续期后键自己消失，避免残留下线节点。
 * 查在线实例用 SCAN，不用 KEYS。
 * 对应文档 docs/16-share.md。
 */
@Service
public class ShareService {

    private final StringRedisTemplate redis;
    private final JsonCodec jsonCodec;

    public ShareService(StringRedisTemplate redis, JsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    public Map<String, Object> put(String field, String value) {
        if (value == null || value.isBlank() || value.length() > 200) {
            throw new BizException("配置值长度需在 1 到 200 之间");
        }
        redis.opsForHash().put(RedisKeys.SHARE_CONFIG, KeyGuard.name(field, "配置项"), value.trim());
        Long version = redis.opsForValue().increment(RedisKeys.SHARE_CONFIG_VER);
        Map<String, Object> body = snapshot();
        body.put("version", version);
        return body;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", number(RedisKeys.SHARE_CONFIG_VER));
        body.put("config", RedisValues.asMap(redis.opsForHash().entries(RedisKeys.SHARE_CONFIG)));
        return body;
    }

    public Map<String, Object> heartbeat(String service, String instance) {
        String serviceName = KeyGuard.name(service, "服务");
        String instanceId = KeyGuard.name(instance, "实例");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", serviceName);
        info.put("instance", instanceId);
        info.put("seenAt", OffsetDateTime.now().toString());
        String key = RedisKeys.serviceInstance(serviceName, instanceId);
        redis.opsForValue().set(key, jsonCodec.toJson(info), Duration.ofSeconds(30));
        info.put("ttlSeconds", 30);
        return info;
    }

    public List<Map<String, Object>> services() {
        List<String> keys = redis.execute((RedisCallback<List<String>>) connection -> {
            List<String> found = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match("share:svc:*").count(100).build();
            Cursor<byte[]> cursor = connection.scan(options);
            try {
                while (cursor.hasNext()) {
                    found.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } finally {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                    // 游标关闭失败不影响已经扫到的键
                }
            }
            return found;
        });
        List<Map<String, Object>> list = new ArrayList<>();
        if (keys == null) {
            return list;
        }
        for (String key : keys) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key);
            row.put("ttlSeconds", redis.getExpire(key));
            row.put("instance", redis.opsForValue().get(key));
            list.add(row);
        }
        return list;
    }

    private long number(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }
}
