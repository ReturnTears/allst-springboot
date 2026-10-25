package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.lifecycle.AdvsLifecycleDemoBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * 只参与示例 Bean 的销毁前回调，不影响其他 Bean 的销毁判断。
 */
@Component
public class AdvsDestructionPostProcessor implements DestructionAwareBeanPostProcessor, PriorityOrdered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return ClassUtils.getUserClass(bean) == AdvsLifecycleDemoBean.class;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (ClassUtils.getUserClass(bean) == AdvsLifecycleDemoBean.class) {
            StartupTraceLog.record(StartupPhase.BEFORE_DESTRUCTION,
                    "销毁前回调，随后才会执行 @PreDestroy。bean=" + beanName);
        }
    }
}
