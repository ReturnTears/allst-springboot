package com.allst.springboot.security;

import com.allst.springboot.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SlidingWindowLimiterTest {

    @Test
    void rejectsWhenWindowIsFull() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.acquire("k", 2, 60);
        limiter.acquire("k", 2, 60);
        assertThrows(BizException.class, () -> limiter.acquire("k", 2, 60));
    }
}
