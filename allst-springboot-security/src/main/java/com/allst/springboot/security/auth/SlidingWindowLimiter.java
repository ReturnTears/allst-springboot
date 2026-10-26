package com.allst.springboot.security.auth;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一分钟内同一个键最多放行 N 次。键是「IP + 路径」，所以打爆登录接口不会把别的接口一起限掉。
 * 计数只在本进程的内存里。多台机器各自计数，部署多实例时要换成共享存储，否则限流会被摊薄。
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
