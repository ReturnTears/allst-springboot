package com.allst.springboot.redis.cache;

import com.allst.springboot.redis.common.BizException;
import com.allst.springboot.redis.common.JsonCodec;
import com.allst.springboot.redis.common.RedisKeys;
import com.allst.springboot.redis.persistence.Product;
import com.allst.springboot.redis.persistence.ProductMapper;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品读走 Cache-Aside，写走「先更新数据库，再删除缓存」。
 * 穿透：布隆过滤器挡住一定不存在的 id，查询结果为空时再缓存短 TTL 的占位值。
 * 击穿：热点键过期时用 SET NX 互斥，只放一个请求回源。
 * 雪崩：TTL 在基础时间上加随机抖动，避免同一批键同时失效。
 * 对应文档 docs/01-cache.md。
 */
@Service
public class CacheAsideService {

    private static final Duration BASE_TTL = Duration.ofMinutes(10);
    private static final Duration NULL_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redis;
    private final ProductMapper productMapper;
    private final JsonCodec jsonCodec;
    private final RedissonClient redissonClient;

    public CacheAsideService(StringRedisTemplate redis,
                             ProductMapper productMapper,
                             JsonCodec jsonCodec,
                             RedissonClient redissonClient) {
        this.redis = redis;
        this.productMapper = productMapper;
        this.jsonCodec = jsonCodec;
        this.redissonClient = redissonClient;
    }

    public Map<String, Object> getProduct(long id, int attempt) {
        if (attempt > 5) {
            throw new BizException("缓存重建排队超时，请稍后重试");
        }
        RBloomFilter<String> bloom = bloom();
        if (bloom.isExists() && !bloom.contains(String.valueOf(id))) {
            redis.opsForValue().increment(RedisKeys.CACHE_STAT_NULL);
            return result(null, "BLOOM");
        }
        String key = RedisKeys.product(id);
        String cached = redis.opsForValue().get(key);
        if (RedisKeys.NULL_VALUE.equals(cached)) {
            redis.opsForValue().increment(RedisKeys.CACHE_STAT_NULL);
            return result(null, "NULL_CACHE");
        }
        if (cached != null) {
            redis.opsForValue().increment(RedisKeys.CACHE_STAT_HIT);
            return result(jsonCodec.fromJson(cached, Product.class), "HIT");
        }
        Boolean locked = redis.opsForValue().setIfAbsent(RedisKeys.productLock(id), "1", Duration.ofSeconds(5));
        if (!Boolean.TRUE.equals(locked)) {
            sleepQuietly();
            return getProduct(id, attempt + 1);
        }
        try {
            cached = redis.opsForValue().get(key);
            if (cached != null && !RedisKeys.NULL_VALUE.equals(cached)) {
                redis.opsForValue().increment(RedisKeys.CACHE_STAT_HIT);
                return result(jsonCodec.fromJson(cached, Product.class), "HIT_AFTER_WAIT");
            }
            redis.opsForValue().increment(RedisKeys.CACHE_STAT_MISS);
            Product product = productMapper.findById(id);
            if (product == null) {
                redis.opsForValue().set(key, RedisKeys.NULL_VALUE, NULL_TTL);
                return result(null, "MISS_NULL");
            }
            redis.opsForValue().set(key, jsonCodec.toJson(product), jitterTtl());
            bloom();
            if (bloom.isExists()) {
                bloom.add(String.valueOf(id));
            }
            return result(product, "MISS");
        } finally {
            redis.delete(RedisKeys.productLock(id));
        }
    }

    public Product update(Product product) {
        if (productMapper.update(product) == 0) {
            throw new BizException("商品不存在");
        }
        redis.delete(RedisKeys.product(product.getId()));
        RBloomFilter<String> bloom = bloom();
        if (bloom.isExists()) {
            bloom.add(String.valueOf(product.getId()));
        }
        return productMapper.findById(product.getId());
    }

    public void rebuildBloom() {
        RBloomFilter<String> bloom = bloom();
        if (!bloom.isExists()) {
            bloom.tryInit(100_000L, 0.01D);
        }
        for (Product product : productMapper.findAll()) {
            bloom.add(String.valueOf(product.getId()));
        }
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("hit", number(RedisKeys.CACHE_STAT_HIT));
        stats.put("miss", number(RedisKeys.CACHE_STAT_MISS));
        stats.put("nullOrBloom", number(RedisKeys.CACHE_STAT_NULL));
        stats.put("bloomReady", bloom().isExists());
        return stats;
    }

    @Cacheable(cacheNames = "dict", key = "'categories'")
    public List<String> categories() {
        return productMapper.categories();
    }

    @CacheEvict(cacheNames = "dict", key = "'categories'")
    public void evictCategories() {
        // 注解负责删除 dict::categories。方法体留空是故意的。
    }

    private RBloomFilter<String> bloom() {
        return redissonClient.getBloomFilter(RedisKeys.CACHE_BLOOM);
    }

    private Duration jitterTtl() {
        return BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(120));
    }

    private long number(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private Map<String, Object> result(Product product, String source) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", source);
        body.put("product", product);
        return body;
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(40L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException("等待缓存重建时被中断");
        }
    }
}
