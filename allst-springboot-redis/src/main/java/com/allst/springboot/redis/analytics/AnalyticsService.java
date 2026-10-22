package com.allst.springboot.redis.analytics;

import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一次访问同时写四份结构：PV 计数、UV 基数、小时桶、页面热度。
 * UV 用 HyperLogLog，允许大约 0.81% 的误差，内存不随用户数线性增长。
 * 对应文档 docs/05-analytics.md。
 */
@Service
public class AnalyticsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redis;

    public AnalyticsService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> hit(String userId, String page) {
        String user = KeyGuard.name(userId, "用户");
        String path = KeyGuard.name(page, "页面");
        LocalDateTime now = LocalDateTime.now();
        String day = now.format(DAY);
        String hour = String.format("%02d", now.getHour());
        redis.opsForValue().increment(RedisKeys.analyticsPv(day));
        redis.opsForHyperLogLog().add(RedisKeys.analyticsUv(day), user);
        redis.opsForHash().increment(RedisKeys.analyticsHour(day), hour, 1);
        redis.opsForZSet().incrementScore(RedisKeys.analyticsHot(day), path, 1);
        return summary(day);
    }

    public Map<String, Object> summary(String day) {
        String date = (day == null || day.isBlank()) ? LocalDate.now().format(DAY) : day;
        if (!date.matches("\\d{8}")) {
            date = LocalDate.now().format(DAY);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("day", date);
        body.put("pv", number(RedisKeys.analyticsPv(date)));
        Long uv = redis.opsForHyperLogLog().size(RedisKeys.analyticsUv(date));
        body.put("uv", uv == null ? 0 : uv);
        body.put("hours", redis.opsForHash().entries(RedisKeys.analyticsHour(date)));
        body.put("hotPages", hot(date));
        return body;
    }

    private List<Map<String, Object>> hot(String day) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.analyticsHot(day), 0, 9);
        List<Map<String, Object>> list = new ArrayList<>();
        if (tuples == null) {
            return list;
        }
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("page", tuple.getValue());
            row.put("hits", tuple.getScore());
            list.add(row);
        }
        return list;
    }

    private long number(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }
}
