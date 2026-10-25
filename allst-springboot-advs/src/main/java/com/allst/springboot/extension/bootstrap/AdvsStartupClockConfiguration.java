package com.allst.springboot.extension.bootstrap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 让 IDE 能解析 {@link AdvsStartupClock} 的注入点。
 * ApplicationContextInitializer 会先用同名 registerSingleton 放入早期实例；
 * 容器发现这个单例已存在时，不会再调用下面的工厂方法。
 */
@Configuration
public class AdvsStartupClockConfiguration {

    @Bean
    public AdvsStartupClock advsStartupClock() {
        return new AdvsStartupClock(System.currentTimeMillis());
    }
}
