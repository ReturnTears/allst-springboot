package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.lifecycle.AdvsLifecycleDemoBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * 初始化前后的加工点。顺序早于 CommonAnnotationBeanPostProcessor，因此先于 @PostConstruct。
 */
@Component
public class AdvsBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (ClassUtils.getUserClass(bean) == AdvsLifecycleDemoBean.class) {
            StartupTraceLog.record(StartupPhase.BEAN_POST_BEFORE_INIT,
                    "初始化前回调，下一步才是 @PostConstruct。bean=" + beanName);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ClassUtils.getUserClass(bean) == AdvsLifecycleDemoBean.class) {
            StartupTraceLog.record(StartupPhase.BEAN_POST_AFTER_INIT,
                    "初始化后回调。AOP 代理通常在此包装；本示例原样返回，不替换 Bean。bean=" + beanName);
        }
        return bean;
    }
}
