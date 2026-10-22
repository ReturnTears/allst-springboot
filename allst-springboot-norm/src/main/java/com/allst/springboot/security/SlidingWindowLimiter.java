package com.allst.springboot.security;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单机滑动窗口。窗口过期的时间戳会被丢掉，超过 permits 直接拒绝。
 * 集群限流应放在网关或 Redis，这里是应用内的最后一道防线。对应规范：4 限流。
 */
@Component
public class SlidingWindowLimiter {

    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public void acquire(String key, int permits, int periodSeconds) {
        if (permits <= 0 || periodSeconds <= 0) {
            throw new IllegalArgumentException("限流参数必须为正数");
        }
        long now = System.currentTimeMillis();
        long windowMillis = periodSeconds * 1000L;
        Deque<Long> queue = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= windowMillis) {
                queue.pollFirst();
            }
            if (queue.size() >= permits) {
                throw new BizException(ResultCode.RATE_LIMIT);
            }
            queue.addLast(now);
        }
    }
}
