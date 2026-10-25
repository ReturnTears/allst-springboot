package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 在配置类解析之后新增 Bean 定义。
 * 第二个方法属于 BeanFactoryPostProcessor 阶段，不再承担注册工作。
 */
@Component
public class AdvsBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor, Ordered {

    static final String FEATURE_SWITCH_BEAN_NAME = "advsFeatureSwitch";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (!registry.containsBeanDefinition(FEATURE_SWITCH_BEAN_NAME)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(AdvsFeatureSwitch.class);
            beanDefinition.setDescription("启动期动态注册的扩展开关，属性由 BeanFactoryPostProcessor 写入");
            registry.registerBeanDefinition(FEATURE_SWITCH_BEAN_NAME, beanDefinition);
        }
        int projectBeans = 0;
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (className != null && className.startsWith("com.allst.springboot.")) {
                projectBeans++;
            }
        }
        StartupTraceLog.record(StartupPhase.BEAN_DEFINITION_REGISTRY,
                "注册 " + FEATURE_SWITCH_BEAN_NAME + "。工程 Bean 定义 " + projectBeans
                        + " 个，总定义 " + registry.getBeanDefinitionCount() + " 个。");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        StartupTraceLog.record(StartupPhase.REGISTRY_AS_FACTORY_POST,
                "注册器的第二个回调。新增定义的时机已经过去，这里只确认 " + FEATURE_SWITCH_BEAN_NAME
                        + " 仍在工厂中=" + beanFactory.containsBeanDefinition(FEATURE_SWITCH_BEAN_NAME) + "。");
    }
}
