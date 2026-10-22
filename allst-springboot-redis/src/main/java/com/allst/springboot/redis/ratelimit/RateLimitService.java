package com.allst.springboot.redis.ratelimit;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 三种限流都放在 Lua 里一次做完，避免「先读再写」在并发下超卖额度。
 * 固定窗口实现简单，但窗口交界可能打进两倍流量。
 * 滑动窗口按请求时间削峰，更平滑。
 * 令牌桶允许短时间突发，之后被补充速率拦住。
 * 对应文档 docs/08-ratelimit.md。
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> fixedWindowScript;
    private final DefaultRedisScript<List> slidingScript;
    private final DefaultRedisScript<List> tokenScript;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
        this.fixedWindowScript = script("lua/fixed_window.lua", Long.class);
        this.slidingScript = script("lua/sliding_window.lua", List.class);
        this.tokenScript = script("lua/token_bucket.lua", List.class);
    }

    public Map<String, Object> fixedWindow(String name, int limit, int windowSeconds) {
        String key = "ratelimit:fixed:" + KeyGuard.name(name, "名称");
        int safeLimit = bound(limit, 1, 1000);
        int safeWindow = bound(windowSeconds, 1, 3600);
        Long current = redis.execute(fixedWindowScript, Collections.singletonList(key), String.valueOf(safeWindow));
        long count = current == null ? 0L : current;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowed", count <= safeLimit);
        body.put("count", count);
        body.put("limit", safeLimit);
        body.put("windowSeconds", safeWindow);
        body.put("ttl", redis.getExpire(key));
        return body;
    }

    public Map<String, Object> slidingWindow(String name, int limit, int windowSeconds) {
        String key = "ratelimit:slide:" + KeyGuard.name(name, "名称");
        int safeLimit = bound(limit, 1, 1000);
        int safeWindow = bound(windowSeconds, 1, 3600);
        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID().toString().substring(0, 8);
        List<?> result = redis.execute(slidingScript, Collections.singletonList(key),
                String.valueOf(now), String.valueOf(safeWindow * 1000L), String.valueOf(safeLimit), member);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowed", result != null && number(result, 0) == 1L);
        body.put("count", result == null ? 0 : number(result, 1));
        body.put("limit", safeLimit);
        body.put("windowSeconds", safeWindow);
        return body;
    }

    public Map<String, Object> tokenBucket(String name, int capacity, double ratePerSecond) {
        String key = "ratelimit:token:" + KeyGuard.name(name, "名称");
        int safeCapacity = bound(capacity, 1, 1000);
        if (ratePerSecond <= 0 || ratePerSecond > 1000) {
            throw new BizException("补充速率需在 0 到 1000 之间");
        }
        List<?> result = redis.execute(tokenScript, Collections.singletonList(key),
                String.valueOf(safeCapacity), String.valueOf(ratePerSecond), String.valueOf(System.currentTimeMillis()));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("allowed", result != null && number(result, 0) == 1L);
        body.put("tokensLeft", result == null || result.size() < 2 ? null : String.valueOf(result.get(1)));
        body.put("capacity", safeCapacity);
        body.put("ratePerSecond", ratePerSecond);
        return body;
    }

    private int bound(int value, int min, int max) {
        if (value < min || value > max) {
            throw new BizException("参数需在 " + min + " 到 " + max + " 之间");
        }
        return value;
    }

    private long number(List<?> result, int index) {
        if (result.size() <= index || result.get(index) == null) {
            return 0L;
        }
        return (long) Double.parseDouble(String.valueOf(result.get(index)));
    }

    private <T> DefaultRedisScript<T> script(String path, Class<T> type) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(type);
        return script;
    }
}
