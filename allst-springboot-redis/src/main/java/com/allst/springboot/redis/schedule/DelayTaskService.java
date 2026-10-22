package com.allst.springboot.redis.schedule;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.RedisKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 延迟任务放在 ZSET 里，分数是执行时间。调度器用 Lua 把到期成员删出来，删成功的实例才执行。
 * 这样多个应用节点可以同时跑调度，不会把同一条任务执行两遍。
 * 对应文档 docs/15-schedule.md。
 */
@Slf4j
@Service
public class DelayTaskService {

    private final StringRedisTemplate redis;
    private final JsonCodec jsonCodec;
    private final DefaultRedisScript<List> claimScript;
    private final AtomicLong lastWarn = new AtomicLong();

    public DelayTaskService(StringRedisTemplate redis, JsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
        this.claimScript = new DefaultRedisScript<>();
        this.claimScript.setLocation(new ClassPathResource("lua/claim_delay.lua"));
        this.claimScript.setResultType(List.class);
    }

    public Map<String, Object> submit(String type, String payload, int delaySeconds) {
        if (!"LOG".equals(type) && !"DELETE_KEY".equals(type)) {
            throw new BizException("任务类型只支持 LOG 或 DELETE_KEY");
        }
        if (delaySeconds < 0 || delaySeconds > 86400) {
            throw new BizException("延迟需在 0 到 86400 秒之间");
        }
        String body = payload == null ? "" : payload.trim();
        if ("DELETE_KEY".equals(type) && !body.matches("temp:[A-Za-z0-9:_\\-]{1,80}")) {
            throw new BizException("DELETE_KEY 的内容必须是 temp: 开头的简单键名");
        }
        if (body.length() > 200) {
            throw new BizException("任务内容最长 200 字");
        }
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", UUID.randomUUID().toString().replace("-", ""));
        task.put("type", type);
        task.put("payload", body);
        long runAt = System.currentTimeMillis() + delaySeconds * 1000L;
        task.put("runAt", runAt);
        redis.opsForZSet().add(RedisKeys.SCHEDULE_DELAY, jsonCodec.toJson(task), runAt);
        return task;
    }

    public List<Map<String, Object>> pending() {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().rangeWithScores(RedisKeys.SCHEDULE_DELAY, 0, 49);
        List<Map<String, Object>> list = new ArrayList<>();
        if (tuples == null) {
            return list;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> row = jsonCodec.fromJson(tuple.getValue(), new TypeReference<Map<String, Object>>() {
            });
            row.put("score", tuple.getScore());
            list.add(row);
        }
        return list;
    }

    public List<String> results() {
        List<String> values = redis.opsForList().range(RedisKeys.SCHEDULE_RESULT, 0, 19);
        return values == null ? Collections.emptyList() : values;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        try {
            List<?> claimed = redis.execute(claimScript,
                    Collections.singletonList(RedisKeys.SCHEDULE_DELAY),
                    String.valueOf(System.currentTimeMillis()),
                    "20");
            if (claimed == null) {
                return;
            }
            for (Object item : claimed) {
                finish(String.valueOf(item));
            }
        } catch (Exception ex) {
            long now = System.currentTimeMillis();
            long previous = lastWarn.get();
            if (now - previous > 30_000 && lastWarn.compareAndSet(previous, now)) {
                log.warn("延迟任务调度暂停: {}", ex.getMessage());
            }
        }
    }

    private void finish(String json) {
        Map<String, Object> task = jsonCodec.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
        String type = String.valueOf(task.get("type"));
        String payload = String.valueOf(task.get("payload"));
        if ("DELETE_KEY".equals(type) && payload.startsWith("temp:")) {
            redis.delete(payload);
            task.put("status", "deleted");
        } else {
            task.put("status", "logged");
        }
        task.put("finishedAt", System.currentTimeMillis());
        redis.opsForList().leftPush(RedisKeys.SCHEDULE_RESULT, jsonCodec.toJson(task));
        redis.opsForList().trim(RedisKeys.SCHEDULE_RESULT, 0, 49);
    }
}
