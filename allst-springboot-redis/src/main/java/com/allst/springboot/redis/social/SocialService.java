package com.allst.springboot.redis.social;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 关注关系用两个 Set：我关注的人、关注我的人。共同关注就是两个关注集合的交集。
 * 发动态时推给粉丝的收件箱，这是写扩散。粉丝量很大时要改成读扩散，示例把粉丝扇出限制在 200 人。
 * 在线状态是一个 60 秒的键，客户端需要定时续期。
 * 对应文档 docs/11-social.md。
 */
@Service
public class SocialService {

    private final StringRedisTemplate redis;
    private final JsonCodec jsonCodec;

    public SocialService(StringRedisTemplate redis, JsonCodec jsonCodec) {
        this.redis = redis;
        this.jsonCodec = jsonCodec;
    }

    public Map<String, Object> follow(String user, String target, boolean follow) {
        String source = KeyGuard.name(user, "用户");
        String dest = KeyGuard.name(target, "目标用户");
        if (source.equals(dest)) {
            throw new BizException("不能关注自己");
        }
        if (follow) {
            redis.opsForSet().add(RedisKeys.follow(source), dest);
            redis.opsForSet().add(RedisKeys.fans(dest), source);
        } else {
            redis.opsForSet().remove(RedisKeys.follow(source), dest);
            redis.opsForSet().remove(RedisKeys.fans(dest), source);
        }
        return profile(source);
    }

    public Set<String> mutual(String a, String b) {
        Set<String> both = redis.opsForSet().intersect(
                RedisKeys.follow(KeyGuard.name(a, "用户")),
                RedisKeys.follow(KeyGuard.name(b, "用户")));
        return both == null ? Collections.emptySet() : both;
    }

    public Map<String, Object> post(String user, String content) {
        String author = KeyGuard.name(user, "用户");
        if (content == null || content.isBlank() || content.length() > 140) {
            throw new BizException("动态长度需在 1 到 140 之间");
        }
        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("author", author);
        feed.put("content", content.trim());
        feed.put("ts", OffsetDateTime.now().toString());
        String json = jsonCodec.toJson(feed);
        push(RedisKeys.feed(author), json);
        Set<String> fans = redis.opsForSet().members(RedisKeys.fans(author));
        int delivered = 0;
        if (fans != null) {
            for (String fan : fans) {
                if (delivered >= 200) {
                    break;
                }
                push(RedisKeys.feed(fan), json);
                delivered++;
            }
        }
        feed.put("delivered", delivered);
        return feed;
    }

    public List<String> feed(String user) {
        List<String> values = redis.opsForList().range(RedisKeys.feed(KeyGuard.name(user, "用户")), 0, 19);
        return values == null ? Collections.emptyList() : values;
    }

    public Map<String, Object> online(String user) {
        String name = KeyGuard.name(user, "用户");
        redis.opsForValue().set(RedisKeys.online(name), OffsetDateTime.now().toString(), Duration.ofSeconds(60));
        return presence(name);
    }

    public Map<String, Object> presence(String user) {
        String name = KeyGuard.name(user, "用户");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", name);
        body.put("online", Boolean.TRUE.equals(redis.hasKey(RedisKeys.online(name))));
        body.put("ttlSeconds", redis.getExpire(RedisKeys.online(name)));
        return body;
    }

    public Map<String, Object> profile(String user) {
        String name = KeyGuard.name(user, "用户");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", name);
        body.put("following", members(RedisKeys.follow(name)));
        body.put("fans", members(RedisKeys.fans(name)));
        body.put("online", Boolean.TRUE.equals(redis.hasKey(RedisKeys.online(name))));
        return body;
    }

    public void seed() {
        follow("ada", "lin", true);
        follow("ada", "max", true);
        follow("lin", "max", true);
        follow("lin", "qiu", true);
        follow("max", "ada", true);
        post("lin", "今天把排行榜改成 ZSET 了");
        online("ada");
    }

    private void push(String key, String json) {
        redis.opsForList().leftPush(key, json);
        redis.opsForList().trim(key, 0, 99);
    }

    private List<String> members(String key) {
        Set<String> values = redis.opsForSet().members(key);
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
