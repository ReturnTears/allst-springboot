package com.allst.springboot.extension.imports;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 配置类解析完成后，把可选模块注册成 Bean 定义。
 */
public class AdvsExtensionRegistrar implements ImportBeanDefinitionRegistrar {

    static final String NOTICE_BEAN_NAME = "advsDynamicNotice";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(NOTICE_BEAN_NAME)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(AdvsDynamicNotice.class);
            beanDefinition.setDescription("由 ImportBeanDefinitionRegistrar 在启动期动态注册");
            registry.registerBeanDefinition(NOTICE_BEAN_NAME, beanDefinition);
        }
        StartupTraceLog.record(StartupPhase.IMPORT_REGISTRAR,
                "已注册 Bean 定义 " + NOTICE_BEAN_NAME + "，该类没有 @Component，不会被扫描重复创建。");
    }
}
