package com.allst.springboot.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * cache 配置类
 * @author YiYa
 * @since 2020-08-23 下午 10:17
 */
@Configuration
public class MyCacheConfig {
    /**
     * 自定义缓存key生成策略
     * @return 结果
     */
    @Bean("myKeyGenerator")
    public KeyGenerator keyGenerator() {
        return (o, method, objects) -> method.getName() + "[" + Collections.singletonList(objects) + "]";
    }

}
