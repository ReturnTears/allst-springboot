package com.allst.springboot.extension.bootstrap;

/**
 * 在 ApplicationContextInitializer 中提前放进容器的启动时钟。
 * 它不是通过 Bean 生命周期创建的，因此不会经过 BeanPostProcessor。
 */
public class AdvsStartupClock {

    private final long epochMillis;

    public AdvsStartupClock(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public long elapsedMillis() {
        return System.currentTimeMillis() - epochMillis;
    }
}
