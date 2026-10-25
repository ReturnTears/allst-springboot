package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动后的缓存预热入口。默认关闭，避免启动阶段强行访问 Redis。
 */
@Component
@Order(10)
public class AdvsCacheWarmupRunner implements ApplicationRunner {

    private final Environment environment;
    private final ApplicationContext applicationContext;

    public AdvsCacheWarmupRunner(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean warmup = environment.getProperty("advs.extension.cache-warmup", Boolean.class, Boolean.FALSE);
        if (!warmup) {
            StartupTraceLog.record(StartupPhase.APPLICATION_RUNNER,
                    "跳过缓存预热。将 advs.extension.cache-warmup 设为 true 后，这里会列出 CacheManager，失败也不中断启动。");
            return;
        }
        String[] names = applicationContext.getBeanNamesForType(CacheManager.class);
        if (names.length == 0) {
            StartupTraceLog.record(StartupPhase.APPLICATION_RUNNER, "已开启缓存预热，但容器中没有 CacheManager。");
            return;
        }
        for (String name : names) {
            try {
                CacheManager cacheManager = applicationContext.getBean(name, CacheManager.class);
                StartupTraceLog.record(StartupPhase.APPLICATION_RUNNER,
                        "CacheManager=" + name + "，缓存名=" + cacheManager.getCacheNames());
            } catch (RuntimeException ex) {
                StartupTraceLog.record(StartupPhase.APPLICATION_RUNNER,
                        "读取 CacheManager " + name + " 失败，已忽略: " + ex.getClass().getSimpleName());
            }
        }
    }
}
