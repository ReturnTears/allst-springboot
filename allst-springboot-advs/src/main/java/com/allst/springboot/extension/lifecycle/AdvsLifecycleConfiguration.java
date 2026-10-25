package com.allst.springboot.extension.lifecycle;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用 @Bean 的 initMethod / destroyMethod 挂上自定义初始化与销毁，避免和接口方法同名而被容器合并。
 */
@Configuration
public class AdvsLifecycleConfiguration {

    @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
    public AdvsLifecycleDemoBean advsLifecycleDemoBean() {
        return new AdvsLifecycleDemoBean();
    }
}
