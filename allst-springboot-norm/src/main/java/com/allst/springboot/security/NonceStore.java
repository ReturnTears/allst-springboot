package com.allst.springboot.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内随机串去重。多实例部署时要换成 Redis，否则重放可以打到另一台机器。
 */
@Component
public class NonceStore {

    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();

    /**
     * @return true 表示这是窗口内第一次出现
     */
    public boolean remember(String appId, String nonce, long ttlMillis) {
        evict(ttlMillis);
        String key = appId + ":" + nonce;
        return seen.putIfAbsent(key, System.currentTimeMillis()) == null;
    }

    private void evict(long ttlMillis) {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : seen.entrySet()) {
            if (now - entry.getValue() > ttlMillis) {
                seen.remove(entry.getKey(), entry.getValue());
            }
        }
    }
}
