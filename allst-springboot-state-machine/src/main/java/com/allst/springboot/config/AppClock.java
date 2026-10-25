package com.allst.springboot.config;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 业务时间入口。测试可以冻结时间，生产路径不会调用 freeze。
 */
@Component
public class AppClock {

    private final AtomicReference<LocalDateTime> frozen = new AtomicReference<>();

    public LocalDateTime now() {
        LocalDateTime value = frozen.get();
        return value == null ? LocalDateTime.now() : value;
    }

    public void freeze(LocalDateTime time) {
        frozen.set(time);
    }

    public void unfreeze() {
        frozen.set(null);
    }
}
