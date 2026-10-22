package com.allst.springboot.redis.expire;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 过期示例只允许操作 temp: 前缀，避免误删业务键。
 * TTL 返回 -1 表示永不过期，-2 表示键不存在。
 * 对应文档 docs/09-expire.md。
 */
@Service
public class ExpireService {

    private final StringRedisTemplate redis;

    public ExpireService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> put(String key, String value, int ttlSeconds) {
        String redisKey = tempKey(key);
        if (value == null || value.isBlank() || value.length() > 200) {
            throw new BizException("值的长度需在 1 到 200 之间");
        }
        if (ttlSeconds < 1 || ttlSeconds > 3600) {
            throw new BizException("过期时间需在 1 到 3600 秒之间");
        }
        redis.opsForValue().set(redisKey, value, Duration.ofSeconds(ttlSeconds));
        return inspect(redisKey);
    }

    public Map<String, Object> inspect(String key) {
        String redisKey = key.startsWith("temp:") ? key : tempKey(key);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", redisKey);
        body.put("value", redis.opsForValue().get(redisKey));
        body.put("ttlSeconds", redis.getExpire(redisKey));
        return body;
    }

    public Map<String, Object> persist(String key) {
        String redisKey = tempKey(key);
        redis.persist(redisKey);
        return inspect(redisKey);
    }

    public List<String> events() {
        List<String> values = redis.opsForList().range(RedisKeys.EXPIRE_LOG, 0, 19);
        return values == null ? Collections.emptyList() : values;
    }

    private String tempKey(String key) {
        String name = KeyGuard.name(key, "键");
        if (name.startsWith("temp:")) {
            return name;
        }
        return "temp:" + name;
    }
}
