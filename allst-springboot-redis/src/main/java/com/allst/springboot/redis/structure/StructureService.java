package com.allst.springboot.redis.structure;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.common.RedisValues;
import com.allst.springboot.redis.common.Step;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 八种数据结构的可重复示例。运行 demo 会先删掉自己的 demo:struct:* 键再重放命令。
 * 对应文档 docs/00-structure.md。
 */
@Service
public class StructureService {

    private final StringRedisTemplate redis;

    public StructureService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public List<Step> demo(String type) {
        switch (normalize(type)) {
            case "string":
                return stringDemo();
            case "hash":
                return hashDemo();
            case "list":
                return listDemo();
            case "set":
                return setDemo();
            case "zset":
                return zsetDemo();
            case "geo":
                return geoDemo();
            case "hyperloglog":
                return hllDemo();
            case "bitmap":
                return bitmapDemo();
            default:
                throw new BizException("未知数据结构: " + type);
        }
    }

    public Object play(String type, Map<String, Object> body) {
        String op = text(body, "op");
        switch (normalize(type)) {
            case "string":
                return playString(op, body);
            case "hash":
                return playHash(op, body);
            case "list":
                return playList(op, body);
            case "set":
                return playSet(op, body);
            case "zset":
                return playZset(op, body);
            case "geo":
                return playGeo(op, body);
            case "hyperloglog":
                return playHll(op, body);
            case "bitmap":
                return playBitmap(op, body);
            default:
                throw new BizException("未知数据结构: " + type);
        }
    }

    private List<Step> stringDemo() {
        String key = RedisKeys.STRUCT_STRING;
        String counter = key + ":views";
        redis.delete(Arrays.asList(key, counter));
        List<Step> steps = new ArrayList<>();
        redis.opsForValue().set(key, "hello");
        steps.add(step("SET demo:struct:string hello", "最基础的键值", redis.opsForValue().get(key)));
        redis.opsForValue().append(key, " redis");
        steps.add(step("APPEND demo:struct:string \" redis\"", "在原值后面追加", redis.opsForValue().get(key)));
        redis.opsForValue().set(key + ":ttl", "临时", Duration.ofSeconds(120));
        steps.add(step("SETEX demo:struct:string:ttl 120 临时", "写入时直接带过期时间", redis.getExpire(key + ":ttl")));
        redis.opsForValue().increment(counter);
        redis.opsForValue().increment(counter, 4);
        steps.add(step("INCR / INCRBY demo:struct:string:views", "计数是原子的，适合浏览量", redis.opsForValue().get(counter)));
        return steps;
    }

    private List<Step> hashDemo() {
        String key = RedisKeys.STRUCT_HASH;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        Map<String, String> user = new LinkedHashMap<>();
        user.put("name", "ada");
        user.put("city", "hangzhou");
        user.put("title", "engineer");
        redis.opsForHash().putAll(key, user);
        steps.add(step("HSET demo:struct:hash name ada city hangzhou title engineer", "一个键里存多个字段，适合对象", RedisValues.asMap(redis.opsForHash().entries(key))));
        redis.opsForHash().increment(key, "visits", 1);
        steps.add(step("HINCRBY demo:struct:hash visits 1", "单个字段也能原子自增", redis.opsForHash().get(key, "visits")));
        steps.add(step("HGET demo:struct:hash city", "只取一个字段，不必把整个对象读出来", redis.opsForHash().get(key, "city")));
        return steps;
    }

    private List<Step> listDemo() {
        String key = RedisKeys.STRUCT_LIST;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        redis.opsForList().rightPushAll(key, "a", "b", "c");
        steps.add(step("RPUSH demo:struct:list a b c", "从右侧入队，配合 LPOP 就是 FIFO", redis.opsForList().range(key, 0, -1)));
        steps.add(step("LPOP demo:struct:list", "弹出最旧的元素", redis.opsForList().leftPop(key)));
        redis.opsForList().leftPush(key, "z");
        steps.add(step("LPUSH demo:struct:list z", "从左侧插入，变成栈顶", redis.opsForList().range(key, 0, -1)));
        redis.opsForList().trim(key, 0, 1);
        steps.add(step("LTRIM demo:struct:list 0 1", "只保留最新的两段，时间线常用", redis.opsForList().range(key, 0, -1)));
        return steps;
    }

    private List<Step> setDemo() {
        redis.delete(Arrays.asList(RedisKeys.STRUCT_SET_A, RedisKeys.STRUCT_SET_B));
        List<Step> steps = new ArrayList<>();
        redis.opsForSet().add(RedisKeys.STRUCT_SET_A, "java", "redis", "mysql");
        redis.opsForSet().add(RedisKeys.STRUCT_SET_B, "redis", "kafka", "mysql");
        steps.add(step("SADD demo:struct:set:a java redis mysql", "集合自动去重", redis.opsForSet().members(RedisKeys.STRUCT_SET_A)));
        steps.add(step("SISMEMBER demo:struct:set:a redis", "判断成员是否存在", redis.opsForSet().isMember(RedisKeys.STRUCT_SET_A, "redis")));
        steps.add(step("SINTER demo:struct:set:a demo:struct:set:b", "交集可以做共同标签、共同好友", redis.opsForSet().intersect(RedisKeys.STRUCT_SET_A, RedisKeys.STRUCT_SET_B)));
        steps.add(step("SUNION demo:struct:set:a demo:struct:set:b", "并集", redis.opsForSet().union(RedisKeys.STRUCT_SET_A, RedisKeys.STRUCT_SET_B)));
        return steps;
    }

    private List<Step> zsetDemo() {
        String key = RedisKeys.STRUCT_ZSET;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        redis.opsForZSet().add(key, "ada", 80);
        redis.opsForZSet().add(key, "lin", 95);
        redis.opsForZSet().add(key, "max", 70);
        redis.opsForZSet().incrementScore(key, "ada", 20);
        steps.add(step("ZADD + ZINCRBY demo:struct:zset", "成员按分数排序，加分是原子的", tuples(key, 10)));
        steps.add(step("ZREVRANK demo:struct:zset ada", "名次从 0 开始，0 是第一名", redis.opsForZSet().reverseRank(key, "ada")));
        return steps;
    }

    private List<Step> geoDemo() {
        String key = RedisKeys.STRUCT_GEO;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        redis.opsForGeo().add(key, new Point(116.4074, 39.9042), "beijing");
        redis.opsForGeo().add(key, new Point(121.4737, 31.2304), "shanghai");
        redis.opsForGeo().add(key, new Point(120.1551, 30.2741), "hangzhou");
        steps.add(step("GEOADD demo:struct:geo 120.1551 30.2741 hangzhou", "底层是 ZSET，分数由经纬度编成", members(redis.opsForZSet().range(key, 0, -1))));
        Distance distance = redis.opsForGeo().distance(key, "hangzhou", "shanghai", Metrics.KILOMETERS);
        steps.add(step("GEODIST demo:struct:geo hangzhou shanghai km", "两点球面距离", distance == null ? null : distance.getValue()));
        steps.add(step("GEORADIUS demo:struct:geo 120.1551 30.2741 200 km", "查杭州附近 200 公里", nearby(key, 120.1551, 30.2741, 200, 10)));
        return steps;
    }

    private List<Step> hllDemo() {
        String key = RedisKeys.STRUCT_HLL;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        String[] users = new String[1000];
        for (int i = 0; i < users.length; i++) {
            users[i] = "user-" + (i % 700);
        }
        redis.opsForHyperLogLog().add(key, users);
        steps.add(step("PFADD demo:struct:hll user-0 ...", "写入 1000 次，实际只有 700 个不同用户", redis.opsForHyperLogLog().size(key)));
        steps.add(step("PFCOUNT demo:struct:hll", "结果是估算值，标准误差约 0.81%，换来的是固定且很小的内存", redis.opsForHyperLogLog().size(key)));
        return steps;
    }

    private List<Step> bitmapDemo() {
        String key = RedisKeys.STRUCT_BITMAP;
        redis.delete(key);
        List<Step> steps = new ArrayList<>();
        redis.opsForValue().setBit(key, 1, true);
        redis.opsForValue().setBit(key, 2, true);
        redis.opsForValue().setBit(key, 5, true);
        steps.add(step("SETBIT demo:struct:bitmap 1|2|5 1", "把第 1、2、5 天标记为已签到", bitCount(key)));
        steps.add(step("GETBIT demo:struct:bitmap 3", "第 3 天没有签到", redis.opsForValue().getBit(key, 3)));
        steps.add(step("BITCOUNT demo:struct:bitmap", "统计 1 的个数，就是签到天数", bitCount(key)));
        return steps;
    }

    private Object playString(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("get".equals(op)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("value", redis.opsForValue().get(key));
            result.put("ttl", redis.getExpire(key));
            return result;
        }
        if ("set".equals(op)) {
            String value = text(body, "value");
            int ttl = intValue(body, "ttl", 0);
            if (ttl > 0) {
                redis.opsForValue().set(key, value, Duration.ofSeconds(ttl));
            } else {
                redis.opsForValue().set(key, value);
            }
            return redis.opsForValue().get(key);
        }
        if ("incr".equals(op)) {
            return redis.opsForValue().increment(key, intValue(body, "delta", 1));
        }
        throw new BizException("string 支持的 op: get, set, incr");
    }

    private Object playHash(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("get".equals(op)) {
            return RedisValues.asMap(redis.opsForHash().entries(key));
        }
        if ("set".equals(op)) {
            redis.opsForHash().put(key, KeyGuard.name(text(body, "field"), "字段"), text(body, "value"));
            return RedisValues.asMap(redis.opsForHash().entries(key));
        }
        throw new BizException("hash 支持的 op: get, set");
    }

    private Object playList(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("range".equals(op)) {
            return redis.opsForList().range(key, 0, -1);
        }
        if ("push".equals(op)) {
            redis.opsForList().leftPush(key, text(body, "value"));
            redis.opsForList().trim(key, 0, 99);
            return redis.opsForList().range(key, 0, -1);
        }
        if ("pop".equals(op)) {
            return redis.opsForList().rightPop(key);
        }
        throw new BizException("list 支持的 op: range, push, pop");
    }

    private Object playSet(String op, Map<String, Object> body) {
        if ("inter".equals(op)) {
            String a = KeyGuard.name(text(body, "a"), "集合 A");
            String b = KeyGuard.name(text(body, "b"), "集合 B");
            return redis.opsForSet().intersect(a, b);
        }
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("members".equals(op)) {
            return redis.opsForSet().members(key);
        }
        if ("add".equals(op)) {
            redis.opsForSet().add(key, text(body, "value"));
            return redis.opsForSet().members(key);
        }
        throw new BizException("set 支持的 op: members, add, inter");
    }

    private Object playZset(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("top".equals(op)) {
            return tuples(key, intValue(body, "limit", 10));
        }
        if ("incr".equals(op)) {
            double delta = body.get("delta") == null ? 1D : Double.parseDouble(String.valueOf(body.get("delta")));
            redis.opsForZSet().incrementScore(key, KeyGuard.name(text(body, "member"), "成员"), delta);
            return tuples(key, 10);
        }
        throw new BizException("zset 支持的 op: top, incr");
    }

    private Object playGeo(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("add".equals(op)) {
            redis.opsForGeo().add(key, new Point(doubleValue(body, "lng"), doubleValue(body, "lat")), KeyGuard.name(text(body, "member"), "成员"));
            return members(redis.opsForZSet().range(key, 0, -1));
        }
        if ("nearby".equals(op)) {
            return nearby(key, doubleValue(body, "lng"), doubleValue(body, "lat"), doubleValue(body, "radiusKm"), intValue(body, "limit", 10));
        }
        throw new BizException("geo 支持的 op: add, nearby");
    }

    private Object playHll(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("add".equals(op)) {
            redis.opsForHyperLogLog().add(key, text(body, "value"));
        } else if (!"count".equals(op)) {
            throw new BizException("hyperloglog 支持的 op: add, count");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("estimate", redis.opsForHyperLogLog().size(key));
        return result;
    }

    private Object playBitmap(String op, Map<String, Object> body) {
        String key = KeyGuard.name(text(body, "key"), "键");
        if ("set".equals(op)) {
            int offset = intValue(body, "offset", -1);
            if (offset < 0 || offset > 100000) {
                throw new BizException("offset 需在 0 到 100000 之间");
            }
            redis.opsForValue().setBit(key, offset, bool(body, "value"));
        } else if (!"stat".equals(op)) {
            throw new BizException("bitmap 支持的 op: set, stat");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bitCount", bitCount(key));
        if (body.get("offset") != null) {
            result.put("bit", redis.opsForValue().getBit(key, intValue(body, "offset", 0)));
        }
        return result;
    }

    private List<Map<String, Object>> nearby(String key, double lng, double lat, double radiusKm, int limit) {
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().radius(
                key, new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)), args);
        List<Map<String, Object>> list = new ArrayList<>();
        if (results == null) {
            return list;
        }
        results.forEach(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("member", item.getContent().getName());
            row.put("distance", item.getDistance() == null ? null : item.getDistance().getValue());
            row.put("metric", item.getDistance() == null ? null : item.getDistance().getMetric().getAbbreviation());
            Point point = item.getContent().getPoint();
            if (point != null) {
                row.put("lng", point.getX());
                row.put("lat", point.getY());
            }
            list.add(row);
        });
        return list;
    }

    private List<Map<String, Object>> tuples(String key, int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, 0, Math.max(limit, 1) - 1L);
        List<Map<String, Object>> list = new ArrayList<>();
        if (tuples == null) {
            return list;
        }
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("member", tuple.getValue());
            row.put("score", tuple.getScore());
            list.add(row);
        }
        return list;
    }

    private Long bitCount(String key) {
        return redis.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                connection.bitCount(key.getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> members(Set<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private Step step(String command, String comment, Object result) {
        return new Step(command, comment, result);
    }

    private String normalize(String type) {
        if ("hll".equalsIgnoreCase(type)) {
            return "hyperloglog";
        }
        return type == null ? "" : type.toLowerCase();
    }

    private String text(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null || String.valueOf(body.get(key)).isBlank()) {
            throw new BizException("缺少参数 " + key);
        }
        return String.valueOf(body.get(key)).trim();
    }

    private int intValue(Map<String, Object> body, String key, int defaultValue) {
        if (body == null || body.get(key) == null || String.valueOf(body.get(key)).isBlank()) {
            return defaultValue;
        }
        return (int) Double.parseDouble(String.valueOf(body.get(key)));
    }

    private double doubleValue(Map<String, Object> body, String key) {
        return Double.parseDouble(text(body, key));
    }

    private boolean bool(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return true;
        }
        return !"false".equalsIgnoreCase(String.valueOf(value)) && !"0".equals(String.valueOf(value));
    }
}
