package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.lifecycle.AdvsConstructorSample;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;

/**
 * 构造器推断和循环依赖早期引用。
 * 推断结果返回 null，让 Spring 继续选择构造器。
 */
@Component
public class AdvsSmartInstantiationPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

    @Override
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        if (beanClass == AdvsConstructorSample.class) {
            StartupTraceLog.record(StartupPhase.DETERMINE_CONSTRUCTOR,
                    "组件扫描 Bean 的构造器继续交给容器推断，回调返回 null。bean=" + beanName);
        }
        return null;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        Class<?> userClass = ClassUtils.getUserClass(bean);
        if (userClass.getName().startsWith("com.allst.")) {
            StartupTraceLog.record(StartupPhase.EARLY_REFERENCE,
                    "Bean 被提前暴露，名称=" + beanName + "，类型=" + userClass.getSimpleName()
                            + "。出现循环依赖时才会走到这里。");
        }
        return bean;
    }
}
