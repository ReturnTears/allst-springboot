package com.allst.springboot.redis.timeseries;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 不依赖 RedisTimeSeries 模块。时间戳放进 ZSET 的分数，数值放进 Hash。
 * 查询先按时间范围取出点，再在应用里按桶做平均。这样任意 Redis 都能跑。
 * 对应文档 docs/14-timeseries.md。
 */
@Service
public class TimeSeriesService {

    private final StringRedisTemplate redis;

    public TimeSeriesService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> add(String metric, double value, long timestamp) {
        String name = KeyGuard.name(metric, "指标");
        if (Double.isNaN(value) || Math.abs(value) > 1_000_000_000D) {
            throw new BizException("数值不合法");
        }
        long ts = timestamp > 0 ? timestamp : System.currentTimeMillis();
        String member = ts + ":" + Long.toUnsignedString(System.nanoTime());
        redis.opsForZSet().add(RedisKeys.tsIndex(name), member, ts);
        redis.opsForHash().put(RedisKeys.tsValue(name), member, String.valueOf(value));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("metric", name);
        body.put("timestamp", ts);
        body.put("value", value);
        return body;
    }

    public Map<String, Object> query(String metric, long from, long to, int bucketSeconds) {
        String name = KeyGuard.name(metric, "指标");
        long start = from > 0 ? from : 0;
        long end = to > 0 ? to : System.currentTimeMillis();
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().rangeByScoreWithScores(RedisKeys.tsIndex(name), start, end);
        List<Map<String, Object>> points = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (points.size() >= 300) {
                    break;
                }
                Object raw = redis.opsForHash().get(RedisKeys.tsValue(name), tuple.getValue());
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("timestamp", tuple.getScore() == null ? null : tuple.getScore().longValue());
                point.put("value", raw == null ? null : Double.parseDouble(String.valueOf(raw)));
                points.add(point);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("metric", name);
        body.put("points", bucketSeconds > 0 ? downsample(points, bucketSeconds) : points);
        return body;
    }

    public long retain(String metric, long before) {
        String name = KeyGuard.name(metric, "指标");
        Set<String> old = redis.opsForZSet().rangeByScore(RedisKeys.tsIndex(name), 0, before);
        redis.opsForZSet().removeRangeByScore(RedisKeys.tsIndex(name), 0, before);
        if (old != null && !old.isEmpty()) {
            redis.opsForHash().delete(RedisKeys.tsValue(name), old.toArray());
        }
        return old == null ? 0 : old.size();
    }

    private List<Map<String, Object>> downsample(List<Map<String, Object>> points, int bucketSeconds) {
        int window = Math.min(Math.max(bucketSeconds, 1), 86400);
        long width = window * 1000L;
        Map<Long, double[]> buckets = new TreeMap<>();
        for (Map<String, Object> point : points) {
            Object ts = point.get("timestamp");
            Object value = point.get("value");
            if (!(ts instanceof Long) || !(value instanceof Double)) {
                continue;
            }
            long bucket = ((Long) ts) / width * width;
            double[] agg = buckets.computeIfAbsent(bucket, key -> new double[2]);
            agg[0] += (Double) value;
            agg[1] += 1;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        buckets.forEach((bucket, agg) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", bucket);
            row.put("value", agg[1] == 0 ? 0 : agg[0] / agg[1]);
            row.put("count", (long) agg[1]);
            result.add(row);
        });
        return result;
    }
}
