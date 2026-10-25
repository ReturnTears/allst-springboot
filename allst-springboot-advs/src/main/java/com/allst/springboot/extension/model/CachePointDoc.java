package com.allst.springboot.extension.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 启动期从已有 Service 上扫描到的缓存注解，供回溯缓存切点。
 */
@Getter
@AllArgsConstructor
public class CachePointDoc {

    private final String beanName;
    private final String className;
    private final String methodName;
    private final String annotation;
    private final String cacheNames;
    private final String key;
    private final String condition;
}
