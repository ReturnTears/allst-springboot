package com.allst.springboot.redis.session;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.common.RedisValues;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 登录态放在 Hash，购物车单独放另一个 Hash。
 * 退出登录只删会话，购物车还在；会话每次访问都把 TTL 重新设为 30 分钟，这就是滑动过期。
 * 示例不校验密码。生产环境应先核对数据库里的口令，再发 token。
 * 对应文档 docs/02-session.md。
 */
@Service
public class SessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;

    public SessionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Map<String, Object> login(String username) {
        String user = KeyGuard.name(username, "用户名");
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> session = new LinkedHashMap<>();
        session.put("userId", user);
        session.put("username", user);
        session.put("loginAt", OffsetDateTime.now().toString());
        String key = RedisKeys.session(token);
        redis.opsForHash().putAll(key, session);
        redis.expire(key, SESSION_TTL);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("userId", user);
        body.put("ttlSeconds", SESSION_TTL.getSeconds());
        return body;
    }

    public Map<String, Object> me(String token) {
        Map<String, String> session = require(token);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session", session);
        body.put("ttlSeconds", redis.getExpire(RedisKeys.session(token)));
        return body;
    }

    public void logout(String token) {
        redis.delete(RedisKeys.session(KeyGuard.token(token)));
    }

    public Map<String, String> addCart(String token, String skuId, int qty) {
        if (qty == 0 || Math.abs(qty) > 99) {
            throw new BizException("数量变化需在 -99 到 99 之间，且不能为 0");
        }
        String userId = require(token).get("userId");
        String sku = KeyGuard.name(skuId, "商品");
        String key = RedisKeys.cart(userId);
        Long current = redis.opsForHash().increment(key, sku, qty);
        if (current == null || current <= 0) {
            redis.opsForHash().delete(key, sku);
        }
        redis.expire(key, CART_TTL);
        return RedisValues.asMap(redis.opsForHash().entries(key));
    }

    public Map<String, String> cart(String token) {
        String userId = require(token).get("userId");
        return RedisValues.asMap(redis.opsForHash().entries(RedisKeys.cart(userId)));
    }

    public Map<String, String> removeCartItem(String token, String skuId) {
        String userId = require(token).get("userId");
        redis.opsForHash().delete(RedisKeys.cart(userId), KeyGuard.name(skuId, "商品"));
        return cart(token);
    }

    private Map<String, String> require(String token) {
        String key = RedisKeys.session(KeyGuard.token(token));
        Map<String, String> session = RedisValues.asMap(redis.opsForHash().entries(key));
        if (session.isEmpty()) {
            throw new BizException("登录已失效，请重新登录");
        }
        redis.expire(key, SESSION_TTL);
        redis.opsForHash().put(key, "lastSeen", OffsetDateTime.now().toString());
        session.put("lastSeen", String.valueOf(redis.opsForHash().get(key, "lastSeen")));
        return session;
    }
}
