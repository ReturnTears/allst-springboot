package com.allst.springboot.extension.model;

/**
 * 启动期从已有 Service 上扫描到的缓存注解，供回溯缓存切点。
 */
public class CachePointDoc {

    private final String beanName;
    private final String className;
    private final String methodName;
    private final String annotation;
    private final String cacheNames;
    private final String key;
    private final String condition;

    public CachePointDoc(String beanName, String className, String methodName, String annotation,
                         String cacheNames, String key, String condition) {
        this.beanName = beanName;
        this.className = className;
        this.methodName = methodName;
        this.annotation = annotation;
        this.cacheNames = cacheNames;
        this.key = key;
        this.condition = condition;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getCacheNames() {
        return cacheNames;
    }

    public String getKey() {
        return key;
    }

    public String getCondition() {
        return condition;
    }
}
