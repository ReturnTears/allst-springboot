package com.allst.springboot.redis.recommend;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.persistence.BehaviorEvent;
import com.allst.springboot.redis.persistence.BehaviorMapper;
import com.allst.springboot.redis.persistence.Product;
import com.allst.springboot.redis.persistence.ProductMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实时推荐分成两份数据：用户对物品的兴趣分，以及物品之间的共现分。
 * 行为同时落 MySQL，方便事后核对；在线计算只读 Redis。
 * 推荐时取用户最感兴趣的若干物品，把它们的相似物品加权求和，再去掉已经看过的。
 * 这是物品协同过滤的最小版本，没有离线计算。
 * 对应文档 docs/12-recommend.md。
 */
@Service
public class RecommendService {

    private static final Map<String, Integer> WEIGHTS = Map.of(
            "view", 1,
            "like", 3,
            "cart", 5,
            "buy", 10
    );

    private final StringRedisTemplate redis;
    private final BehaviorMapper behaviorMapper;
    private final ProductMapper productMapper;

    public RecommendService(StringRedisTemplate redis, BehaviorMapper behaviorMapper, ProductMapper productMapper) {
        this.redis = redis;
        this.behaviorMapper = behaviorMapper;
        this.productMapper = productMapper;
    }

    public Map<String, Object> record(String userId, long itemId, String action) {
        String user = KeyGuard.name(userId, "用户");
        String act = action == null ? "" : action.trim().toLowerCase();
        Integer weight = WEIGHTS.get(act);
        if (weight == null) {
            throw new BizException("行为只支持 view、like、cart、buy");
        }
        if (productMapper.findById(itemId) == null) {
            throw new BizException("商品不存在");
        }
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(user);
        event.setItemId(itemId);
        event.setAction(act);
        event.setScore(weight);
        behaviorMapper.insert(event);

        String item = String.valueOf(itemId);
        redis.opsForZSet().incrementScore(RedisKeys.interest(user), item, weight);
        redis.opsForSet().add(RedisKeys.seen(user), item);
        List<String> recent = redis.opsForList().range(RedisKeys.recentItems(user), 0, 4);
        if (recent != null) {
            for (String other : recent) {
                if (!item.equals(other)) {
                    redis.opsForZSet().incrementScore(RedisKeys.similar(Long.parseLong(other)), item, weight);
                    redis.opsForZSet().incrementScore(RedisKeys.similar(itemId), other, weight);
                }
            }
        }
        redis.opsForList().leftPush(RedisKeys.recentItems(user), item);
        redis.opsForList().trim(RedisKeys.recentItems(user), 0, 19);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user);
        body.put("itemId", itemId);
        body.put("action", act);
        body.put("score", weight);
        return body;
    }

    public List<Map<String, Object>> recommend(String userId, int size) {
        String user = KeyGuard.name(userId, "用户");
        int limit = Math.min(Math.max(size, 1), 10);
        Set<ZSetOperations.TypedTuple<String>> interests =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.interest(user), 0, 4);
        Map<String, Double> scores = new HashMap<>();
        if (interests != null) {
            for (ZSetOperations.TypedTuple<String> interest : interests) {
                if (interest.getValue() == null || interest.getScore() == null) {
                    continue;
                }
                Set<ZSetOperations.TypedTuple<String>> similar = redis.opsForZSet()
                        .reverseRangeWithScores(RedisKeys.similar(Long.parseLong(interest.getValue())), 0, 9);
                if (similar == null) {
                    continue;
                }
                for (ZSetOperations.TypedTuple<String> item : similar) {
                    if (item.getValue() == null || item.getScore() == null) {
                        continue;
                    }
                    scores.merge(item.getValue(), item.getScore() * interest.getScore(), Double::sum);
                }
            }
        }
        Set<String> seen = redis.opsForSet().members(RedisKeys.seen(user));
        if (seen != null) {
            seen.forEach(scores::remove);
        }
        List<Map.Entry<String, Double>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort(Comparator.comparingDouble((Map.Entry<String, Double> entry) -> entry.getValue()).reversed());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : ranked) {
            if (result.size() >= limit) {
                break;
            }
            Product product = productMapper.findById(Long.parseLong(entry.getKey()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", Long.parseLong(entry.getKey()));
            row.put("name", product == null ? null : product.getName());
            row.put("score", entry.getValue());
            result.add(row);
        }
        return result;
    }

    public List<BehaviorEvent> latest(String userId) {
        return behaviorMapper.latest(KeyGuard.name(userId, "用户"));
    }

    public void seed() {
        record("ada", 1, "view");
        record("ada", 2, "view");
        record("ada", 5, "like");
        record("lin", 1, "view");
        record("lin", 2, "cart");
        record("lin", 3, "buy");
        record("max", 2, "view");
        record("max", 5, "buy");
        record("max", 6, "view");
    }
}
