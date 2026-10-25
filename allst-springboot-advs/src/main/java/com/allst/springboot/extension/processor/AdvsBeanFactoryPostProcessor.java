package com.allst.springboot.extension.processor;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 在实例化之前修改已有 Bean 定义。
 */
@Component
public class AdvsBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private Environment environment;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBeanDefinition(AdvsBeanDefinitionRegistryPostProcessor.FEATURE_SWITCH_BEAN_NAME)) {
            StartupTraceLog.record(StartupPhase.BEAN_FACTORY_POST_PROCESSOR,
                    "未找到 advsFeatureSwitch，跳过属性改写。");
            return;
        }
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(
                AdvsBeanDefinitionRegistryPostProcessor.FEATURE_SWITCH_BEAN_NAME);
        String banner = environment.getProperty("advs.extension.banner", "ALLST Spring Extension Points");
        boolean cacheWarmup = Boolean.parseBoolean(environment.getProperty("advs.extension.cache-warmup", "false"));
        beanDefinition.getPropertyValues().add("banner", banner);
        beanDefinition.getPropertyValues().add("cacheWarmup", cacheWarmup);
        StartupTraceLog.record(StartupPhase.BEAN_FACTORY_POST_PROCESSOR,
                "实例化前写入 advsFeatureSwitch：banner=" + banner + ", cacheWarmup=" + cacheWarmup);
    }
}
