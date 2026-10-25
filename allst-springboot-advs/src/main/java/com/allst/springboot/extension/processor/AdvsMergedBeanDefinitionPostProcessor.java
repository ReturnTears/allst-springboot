package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.model.CachePointDoc;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * 在属性填充前读取合并后的 Bean 定义，扫描当前工程已有的缓存注解。
 */
@Component
public class AdvsMergedBeanDefinitionPostProcessor implements MergedBeanDefinitionPostProcessor, PriorityOrdered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 3;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType == null || !beanType.getName().startsWith("com.allst.springboot.")) {
            return;
        }
        if (beanType.getName().startsWith("com.allst.springboot.extension.")) {
            return;
        }
        try {
            scan(beanType, beanName);
        } catch (RuntimeException ex) {
            StartupTraceLog.record(StartupPhase.MERGED_BEAN_DEFINITION,
                    "扫描缓存注解失败，bean=" + beanName + "，原因=" + ex.getClass().getSimpleName());
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private void scan(Class<?> beanType, String beanName) {
        CacheConfig cacheConfig = AnnotatedElementUtils.findMergedAnnotation(beanType, CacheConfig.class);
        if (cacheConfig != null) {
            StartupTraceLog.addCachePoint(new CachePointDoc(
                    beanName,
                    beanType.getName(),
                    "-",
                    "CacheConfig",
                    join(cacheConfig.cacheNames()),
                    "",
                    ""));
            StartupTraceLog.record(StartupPhase.MERGED_BEAN_DEFINITION,
                    "发现类级 @CacheConfig，bean=" + beanName + "，cacheNames=" + join(cacheConfig.cacheNames()));
        }
        ReflectionUtils.doWithMethods(beanType, method -> collectMethod(beanName, beanType, method));
    }

    private void collectMethod(String beanName, Class<?> beanType, Method method) {
        if (method.getDeclaringClass() == Object.class || method.isSynthetic()) {
            return;
        }
        Cacheable cacheable = AnnotatedElementUtils.findMergedAnnotation(method, Cacheable.class);
        if (cacheable != null) {
            add(beanName, beanType, method, "Cacheable", join(cacheable.cacheNames()), cacheable.key(), cacheable.condition());
        }
        CacheEvict cacheEvict = AnnotatedElementUtils.findMergedAnnotation(method, CacheEvict.class);
        if (cacheEvict != null) {
            add(beanName, beanType, method, "CacheEvict", join(cacheEvict.cacheNames()), cacheEvict.key(), cacheEvict.condition());
        }
        CachePut cachePut = AnnotatedElementUtils.findMergedAnnotation(method, CachePut.class);
        if (cachePut != null) {
            add(beanName, beanType, method, "CachePut", join(cachePut.cacheNames()), cachePut.key(), cachePut.condition());
        }
        Caching caching = AnnotatedElementUtils.findMergedAnnotation(method, Caching.class);
        if (caching == null) {
            return;
        }
        for (Cacheable item : caching.cacheable()) {
            add(beanName, beanType, method, "Caching.cacheable", join(item.cacheNames()), item.key(), item.condition());
        }
        for (CachePut item : caching.put()) {
            add(beanName, beanType, method, "Caching.put", join(item.cacheNames()), item.key(), item.condition());
        }
        for (CacheEvict item : caching.evict()) {
            add(beanName, beanType, method, "Caching.evict", join(item.cacheNames()), item.key(), item.condition());
        }
    }

    private void add(String beanName, Class<?> beanType, Method method, String annotation,
                     String cacheNames, String key, String condition) {
        StartupTraceLog.addCachePoint(new CachePointDoc(
                beanName,
                beanType.getName(),
                method.getName(),
                annotation,
                cacheNames,
                key,
                condition));
        StartupTraceLog.record(StartupPhase.MERGED_BEAN_DEFINITION,
                "缓存切点 " + beanName + "#" + method.getName() + " @" + annotation
                        + " cacheNames=" + cacheNames + " key=" + key);
    }

    private String join(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return String.join(",", values);
    }
}
