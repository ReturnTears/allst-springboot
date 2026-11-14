package com.allst.springboot.structural.decorator;

import com.allst.springboot.structural.decorator.PriceQuoteDecorators.BaseQuote;
import com.allst.springboot.structural.decorator.PriceQuoteDecorators.CachedQuote;
import com.allst.springboot.structural.decorator.PriceQuoteDecorators.PriceQuote;
import com.allst.springboot.structural.decorator.PriceQuoteDecorators.TimedQuote;
import com.allst.springboot.support.KeyValueStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 组装装饰链的入口：Timed( Cached( Base ) )。
 * 调用方拿到的仍是 PriceQuote 接口，不知道中间有几层。
 */
@Component
public class PriceQuoteService {

    private final PriceQuote quote;
    private final BaseQuote base;
    private final CachedQuote cached;
    private final TimedQuote timed;
    private final KeyValueStore store;

    public PriceQuoteService(KeyValueStore store) {
        this.store = store;
        this.base = new BaseQuote();
        this.cached = new CachedQuote(base, store);
        this.timed = new TimedQuote(cached);
        this.quote = timed;
    }

    public BigDecimal quote(String sku) {
        return quote.quote(sku);
    }

    /** 商品调价时让缓存失效，装饰链不用重建；测试也靠它隔离其它用例的预热 */
    public void evict(String sku) {
        store.delete("dm:price:" + sku);
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dbReads", base.dbReads());
        stats.put("cacheHits", cached.hits());
        stats.put("cacheMisses", cached.misses());
        stats.put("calls", timed.calls());
        stats.put("totalMillis", timed.totalMillis());
        return stats;
    }
}
