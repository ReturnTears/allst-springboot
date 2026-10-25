package com.allst.springboot.common.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 按业务键分段的 JVM 内锁。同一键上的事件串行执行，不同键互不阻塞。
 * 锁对象会随键积累，适合示例；多实例部署应换成按同一键的分布式锁。
 */
@Component
public class KeyStripedLock {

    private final ConcurrentHashMap<String, Object> stripes = new ConcurrentHashMap<>();

    public <T> T call(String key, Supplier<T> action) {
        Object stripe = stripes.computeIfAbsent(key, ignored -> new Object());
        synchronized (stripe) {
            return action.get();
        }
    }
}
