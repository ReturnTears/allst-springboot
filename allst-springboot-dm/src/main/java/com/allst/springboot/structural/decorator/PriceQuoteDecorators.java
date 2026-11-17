package com.allst.springboot.structural.decorator;

import com.allst.springboot.support.KeyValueStore;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 装饰器（Decorator）——给报价服务叠加缓存和耗时统计，不改原实现。
 * <Pre>
 * 场景：商品价格来自一次较慢的远程/DB 查询，后来要加缓存，再后来要加监控。
 * 每次都去改 BaseQuote，会让它同时承担定价、缓存、埋点三件事；
 * 装饰器可以按需一层层套，且顺序可调（先缓存还是先计时，结果不同）。
 * <Pre>
 * Spring 里的 @Cacheable、@Transactional、AOP 日志切面本质都是代理/装饰；
 * MyBatis 的 CachingExecutor 也是给 Executor 套了一层缓存装饰器。
 */
public final class PriceQuoteDecorators {

    private PriceQuoteDecorators() {
    }

    public interface PriceQuote {
        BigDecimal quote(String sku);
    }

    /** 最内层：只负责算价格，模拟一次昂贵查询 */
    public static final class BaseQuote implements PriceQuote {
        private final AtomicInteger dbReads = new AtomicInteger();
        private final Map<String, BigDecimal> catalog = new LinkedHashMap<>();

        public BaseQuote() {
            catalog.put("SKU-PHONE", new BigDecimal("2999.00"));
            catalog.put("SKU-TV", new BigDecimal("1899.00"));
            catalog.put("SKU-GIFT", new BigDecimal("9.90"));
            catalog.put("SKU-BOOK", new BigDecimal("59.00"));
        }

        @Override
        public BigDecimal quote(String sku) {
            dbReads.incrementAndGet();
            BigDecimal price = catalog.get(sku);
            if (price == null) {
                throw new IllegalArgumentException("无此 SKU: " + sku);
            }
            return price;
        }

        public int dbReads() {
            return dbReads.get();
        }
    }

    public abstract static class Decorator implements PriceQuote {
        protected final PriceQuote delegate;

        protected Decorator(PriceQuote delegate) {
            this.delegate = delegate;
        }
    }

    /** 缓存装饰：KeyValueStore 默认内存实现，dm.redis.enabled=true 时切到 Redis */
    public static final class CachedQuote extends Decorator {
        private static final long TTL_SECONDS = 60;
        private final KeyValueStore store;
        private final AtomicInteger hits = new AtomicInteger();
        private final AtomicInteger misses = new AtomicInteger();

        public CachedQuote(PriceQuote delegate, KeyValueStore store) {
            super(delegate);
            this.store = store;
        }

        @Override
        public BigDecimal quote(String sku) {
            String cached = store.get("dm:price:" + sku);
            if (cached != null) {
                hits.incrementAndGet();
                return new BigDecimal(cached);
            }
            misses.incrementAndGet();
            BigDecimal price = delegate.quote(sku);
            store.put("dm:price:" + sku, price.toPlainString(), TTL_SECONDS);
            return price;
        }

        public int hits() {
            return hits.get();
        }

        public int misses() {
            return misses.get();
        }
    }

    /** 耗时装饰：纯横切逻辑，不碰业务结果 */
    public static final class TimedQuote extends Decorator {
        private final AtomicLong calls = new AtomicLong();
        private final AtomicLong totalNanos = new AtomicLong();

        public TimedQuote(PriceQuote delegate) {
            super(delegate);
        }

        @Override
        public BigDecimal quote(String sku) {
            long start = System.nanoTime();
            try {
                return delegate.quote(sku);
            } finally {
                totalNanos.addAndGet(System.nanoTime() - start);
                calls.incrementAndGet();
            }
        }

        public long calls() {
            return calls.get();
        }

        public long totalMillis() {
            return totalNanos.get() / 1_000_000;
        }
    }
}
