package com.allst.springboot.redis.lock;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.KeyGuard;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.persistence.Product;
import com.allst.springboot.redis.persistence.ProductMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 扣库存用 Redisson 锁，不设租约时间，看门狗会给仍在执行的持有者续期。
 * 手写锁用 SET key token NX PX，解锁用 Lua 比对 token，避免删掉别人的锁。
 * 锁粒度是商品 id。锁里面只做数据库条件更新，库存不足时 SQL 影响行数为 0。
 * 对应文档 docs/06-lock.md。
 */
@Service
public class StockLockService {

    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> unlockScript;

    public StockLockService(RedissonClient redissonClient, ProductMapper productMapper, StringRedisTemplate redis) {
        this.redissonClient = redissonClient;
        this.productMapper = productMapper;
        this.redis = redis;
        this.unlockScript = new DefaultRedisScript<>();
        this.unlockScript.setLocation(new ClassPathResource("lua/unlock.lua"));
        this.unlockScript.setResultType(Long.class);
    }

    public Map<String, Object> deduct(long productId, int qty) {
        if (qty < 1 || qty > 20) {
            throw new BizException("单次扣减数量需在 1 到 20 之间");
        }
        RLock lock = redissonClient.getLock("lock:stock:" + productId);
        boolean locked;
        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("等待锁时被中断");
        }
        if (!locked) {
            throw new BizException("有其它请求正在扣减该商品，请稍后重试");
        }
        try {
            int rows = productMapper.deductStock(productId, qty);
            if (rows == 0) {
                throw new BizException("库存不足或商品不存在");
            }
            redis.delete(RedisKeys.product(productId));
            Product product = productMapper.findById(productId);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("productId", productId);
            body.put("qty", qty);
            body.put("stock", product == null ? null : product.getStock());
            body.put("lock", "redisson");
            return body;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Map<String, Object> tryManual(String key, int ttlSeconds) {
        String lockKey = "lock:manual:" + KeyGuard.name(key, "锁名");
        int ttl = Math.min(Math.max(ttlSeconds, 1), 60);
        String token = UUID.randomUUID().toString().replace("-", "");
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(ttl));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locked", Boolean.TRUE.equals(ok));
        body.put("key", lockKey);
        body.put("token", Boolean.TRUE.equals(ok) ? token : null);
        body.put("ttlSeconds", ttl);
        return body;
    }

    public Map<String, Object> unlockManual(String key, String token) {
        String lockKey = "lock:manual:" + KeyGuard.name(key, "锁名");
        Long removed = redis.execute(unlockScript, Collections.singletonList(lockKey), token);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("released", removed != null && removed == 1L);
        return body;
    }
}
