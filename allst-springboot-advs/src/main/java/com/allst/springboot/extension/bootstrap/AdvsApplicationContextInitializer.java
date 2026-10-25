package com.allst.springboot.extension.bootstrap;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * refresh 之前的上下文初始化器。必须通过 spring.factories 注册。
 */
public class AdvsApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        AdvsStartupClock clock = new AdvsStartupClock(System.currentTimeMillis());
        applicationContext.getBeanFactory().registerSingleton("advsStartupClock", clock);
        StartupTraceLog.record(StartupPhase.CONTEXT_INITIALIZER,
                "refresh 前注册单例 advsStartupClock。该对象不会再经过 BeanPostProcessor。");
    }
}
