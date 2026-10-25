package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupParticipant;
import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import com.allst.springboot.extension.lifecycle.AdvsLifecycleDemoBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * 观察示例 Bean 的实例化前后。对其他 Bean 保持默认行为。
 */
@Component
public class AdvsInstantiationPostProcessor implements InstantiationAwareBeanPostProcessor {

    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        if (!participant(beanClass)) {
            return null;
        }
        StartupTraceLog.record(StartupPhase.BEFORE_INSTANTIATION,
                "示例 Bean 构造前。返回 null，继续走容器默认实例化。bean=" + beanName);
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        if (participant(ClassUtils.getUserClass(bean))) {
            StartupTraceLog.record(StartupPhase.AFTER_INSTANTIATION,
                    "实例已创建，即将填充属性。必须返回 true，否则依赖注入会被取消。bean=" + beanName);
        }
        return true;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        if (participant(ClassUtils.getUserClass(bean))) {
            int propertyCount = pvs == null ? 0 : pvs.size(); // PropertyValues may not have size()
            StartupTraceLog.record(StartupPhase.PROPERTY_POST_PROCESS,
                    "属性填充前保持原值。bean=" + beanName + "，属性是否存在=" + (pvs != null));
        }
        return pvs;
    }

    private boolean participant(Class<?> type) {
        return type == AdvsLifecycleDemoBean.class
                || AnnotatedElementUtils.hasAnnotation(type, StartupParticipant.class);
    }
}
