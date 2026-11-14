package com.allst.springboot.creational.singleton;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单例（Singleton）——促销/风控阈值字典的进程内缓存。
 *
 * 场景：一次大促里，折扣门槛、风控金额线这类规则被上百个下单请求反复读取，
 * 但不该每个请求都查一次库。真实项目里对应配置中心客户端的规则缓存。
 *
 * 在 Spring 里不要手写 static getInstance()：@Component 默认就是容器级单例，
 * 手写单例会绕过依赖注入，还会让测试无法替换。真正要处理的是两件事：
 *   1) 单例对象里的可变状态必须线程安全（这里用 ConcurrentHashMap + 不可变快照）；
 *   2) 重新加载和读取之间的可见性（这里用整体替换 + volatile 引用语义的 map）。
 */
@Component
public class RuleRegistry {

    private final ConcurrentHashMap<String, BigDecimal> rules = new ConcurrentHashMap<>();
    private final AtomicLong reloadCount = new AtomicLong();

    public RuleRegistry(@Value("${dm.promotion.vip-discount:0.85}") BigDecimal vipDiscount,
                        @Value("${dm.promotion.full-reduction-threshold:100}") BigDecimal reductionThreshold,
                        @Value("${dm.promotion.full-reduction-amount:20}") BigDecimal reductionAmount,
                        @Value("${dm.risk.high-amount-threshold:5000}") BigDecimal highAmountThreshold) {
        rules.put("VIP_DISCOUNT", vipDiscount);
        rules.put("FULL_REDUCTION_THRESHOLD", reductionThreshold);
        rules.put("FULL_REDUCTION_AMOUNT", reductionAmount);
        rules.put("HIGH_AMOUNT_THRESHOLD", highAmountThreshold);
    }

    public BigDecimal get(String ruleKey) {
        BigDecimal value = rules.get(ruleKey);
        if (value == null) {
            throw new IllegalArgumentException("未定义的规则: " + ruleKey);
        }
        return value;
    }

    /** 配置中心推送时调用：一次替换一批规则，不让读到改了一半的字典 */
    public void reload(Map<String, BigDecimal> fresh) {
        fresh.forEach((k, v) -> rules.put(k, v));
        reloadCount.incrementAndGet();
    }

    public Map<String, BigDecimal> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(rules));
    }

    public long reloadCount() {
        return reloadCount.get();
    }
}
