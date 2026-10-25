package com.allst.springboot.extension.processor;

/**
 * 由 BeanDefinitionRegistryPostProcessor 注册、再由 BeanFactoryPostProcessor 填入属性的开关对象。
 */
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
