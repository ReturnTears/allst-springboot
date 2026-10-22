package com.allst.springboot.redis.rank;

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

/**
 * 排行榜用 ZSET：分数变化和名次查询都是原子的。
 * 浏览量用 INCR。点赞先 SADD 去重，只有新用户才给计数加一，取消赞则 SREM 后再减一。
 * 对应文档 docs/03-rank.md。
 */
@Service
public class RankService {

    private final StringRedisTemplate redis;

    public RankService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> addScore(String user, double delta) {
        String member = KeyGuard.name(user, "用户");
        if (Math.abs(delta) > 1_000_000) {
            throw new BizException("单次分数变化过大");
        }
        Double score = redis.opsForZSet().incrementScore(RedisKeys.RANK_BOARD, member, delta);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", member);
        body.put("score", score);
        body.put("rank", rankOf(member));
        return body;
    }

    public List<Map<String, Object>> top(int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(RedisKeys.RANK_BOARD, 0, size - 1L);
        List<Map<String, Object>> list = new ArrayList<>();
        if (tuples == null) {
            return list;
        }
        long rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("user", tuple.getValue());
            row.put("score", tuple.getScore());
            list.add(row);
        }
        return list;
    }

    public Map<String, Object> me(String user) {
        String member = KeyGuard.name(user, "用户");
        Double score = redis.opsForZSet().score(RedisKeys.RANK_BOARD, member);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", member);
        body.put("score", score);
        body.put("rank", score == null ? null : rankOf(member));
        return body;
    }

    public void seed() {
        redis.delete(RedisKeys.RANK_BOARD);
        redis.opsForZSet().add(RedisKeys.RANK_BOARD, "ada", 120);
        redis.opsForZSet().add(RedisKeys.RANK_BOARD, "lin", 96);
        redis.opsForZSet().add(RedisKeys.RANK_BOARD, "max", 88);
        redis.opsForZSet().add(RedisKeys.RANK_BOARD, "qiu", 77);
        redis.opsForZSet().add(RedisKeys.RANK_BOARD, "su", 64);
    }

    public long view(long articleId) {
        Long value = redis.opsForValue().increment(RedisKeys.articleView(articleId));
        return value == null ? 0L : value;
    }

    public Map<String, Object> like(long articleId, String user, boolean liked) {
        String member = KeyGuard.name(user, "用户");
        String usersKey = RedisKeys.articleLikeUsers(articleId);
        String countKey = RedisKeys.articleLikeCount(articleId);
        boolean changed;
        if (liked) {
            Long added = redis.opsForSet().add(usersKey, member);
            changed = added != null && added == 1L;
            if (changed) {
                redis.opsForValue().increment(countKey);
            }
        } else {
            Long removed = redis.opsForSet().remove(usersKey, member);
            changed = removed != null && removed == 1L;
            if (changed) {
                redis.opsForValue().increment(countKey, -1);
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changed", changed);
        body.put("liked", Boolean.TRUE.equals(redis.opsForSet().isMember(usersKey, member)));
        body.put("count", count(countKey));
        body.put("views", count(RedisKeys.articleView(articleId)));
        return body;
    }

    public Map<String, Object> counter(long articleId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("views", count(RedisKeys.articleView(articleId)));
        body.put("likes", count(RedisKeys.articleLikeCount(articleId)));
        return body;
    }

    private long rankOf(String member) {
        Long rank = redis.opsForZSet().reverseRank(RedisKeys.RANK_BOARD, member);
        return rank == null ? -1 : rank + 1;
    }

    private long count(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }
}
