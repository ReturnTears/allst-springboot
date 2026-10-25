package com.allst.springboot.extension.processor;

import org.springframework.stereotype.Component;

/**
 * 扩展开关。标成组件后，IDE 能识别构造器注入。
 * BeanFactoryPostProcessor 仍会在实例化前改写 banner 和 cacheWarmup。
 */
@Component
public class AdvsFeatureSwitch {

    private String banner;
    private boolean cacheWarmup;

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public boolean isCacheWarmup() {
        return cacheWarmup;
    }

    public void setCacheWarmup(boolean cacheWarmup) {
        this.cacheWarmup = cacheWarmup;
    }
}
