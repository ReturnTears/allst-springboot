package com.allst.springboot.security.auth;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单机滑动窗口。生产环境要换成 Redis 等共享存储，否则多实例各自计数。见 docs/03-attack-protection.md。
 */
public class SlidingWindowLimiter {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int limit, Duration window) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            long threshold = now - window.toMillis();
            while (!deque.isEmpty() && deque.peekFirst() < threshold) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
