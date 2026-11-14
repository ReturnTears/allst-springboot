package com.allst.springboot.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 实现。把 dm.redis.enabled 改成 true 后启用，需要本机 6379 可连。
 * putIfAbsent 用的是 SETNX + EXPIRE，对应命令执行器里的幂等去重。
 */
@Component
@ConditionalOnProperty(name = "dm.redis.enabled", havingValue = "true")
public class RedisKeyValueStore implements KeyValueStore {

    private final StringRedisTemplate redis;

    public RedisKeyValueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void put(String key, String value, long ttlSeconds) {
        redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean putIfAbsent(String key, String value, long ttlSeconds) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }
}
