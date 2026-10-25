package com.allst.springboot.extension.lifecycle;

import com.allst.springboot.extension.StartupParticipant;
import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.StartupTraceLog;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 用同一个 Bean 串起初始化顺序，方便对照轨迹。
 * <p>
 * 期望顺序：BeanNameAware、BeanClassLoaderAware、BeanFactoryAware、
 * EnvironmentAware、ApplicationContextAware、BeanPostProcessor 初始化前、
 * {@code @PostConstruct}、InitializingBean、customInit、BeanPostProcessor 初始化后。
 * 全部单例结束后才会到 {@link #afterSingletonsInstantiated()}。
 * 关闭时顺序是销毁前回调、{@code @PreDestroy}、DisposableBean、customDestroy。
 */
@StartupParticipant
public class AdvsLifecycleDemoBean implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware,
        EnvironmentAware, ApplicationContextAware, InitializingBean, DisposableBean, SmartInitializingSingleton {

    public AdvsLifecycleDemoBean() {
        StartupTraceLog.record(StartupPhase.CONSTRUCTOR, "AdvsLifecycleDemoBean 构造器执行，依赖尚未注入。");
    }

    @Override
    public void setBeanName(String name) {
        StartupTraceLog.record(StartupPhase.AWARE_BEAN_NAME, "Bean 名=" + name);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        StartupTraceLog.record(StartupPhase.AWARE_CLASS_LOADER,
                "类加载器=" + classLoader.getClass().getSimpleName());
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        StartupTraceLog.record(StartupPhase.AWARE_BEAN_FACTORY,
                "已获得 BeanFactory，类型=" + beanFactory.getClass().getSimpleName() + "。此阶段不主动 getBean。");
    }

    @Override
    public void setEnvironment(Environment environment) {
        StartupTraceLog.record(StartupPhase.AWARE_ENVIRONMENT,
                "Environment 已可读，server.port=" + environment.getProperty("server.port")
                        + "，profiles=" + String.join(",", environment.getActiveProfiles()));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        boolean clockReady = applicationContext.containsBean("advsStartupClock");
        StartupTraceLog.record(StartupPhase.AWARE_APPLICATION_CONTEXT,
                "已获得 ApplicationContext，advsStartupClock 存在=" + clockReady
                        + "。其他单例可能尚未创建完，这里不做全量自检。");
    }

    @PostConstruct
    public void postConstruct() {
        StartupTraceLog.record(StartupPhase.POST_CONSTRUCT, "@PostConstruct 执行，早于 InitializingBean。");
    }

    @Override
    public void afterPropertiesSet() {
        StartupTraceLog.record(StartupPhase.INITIALIZING_BEAN, "InitializingBean.afterPropertiesSet 执行。");
    }

    public void customInit() {
        StartupTraceLog.record(StartupPhase.CUSTOM_INIT_METHOD, "自定义 init-method customInit 执行。");
    }

    @Override
    public void afterSingletonsInstantiated() {
        StartupTraceLog.record(StartupPhase.SMART_INITIALIZING_SINGLETON,
                "示例 Bean 收到全部单例已创建的回调。有先后依赖的步骤不要放在这个接口的多个实现之间。");
    }

    @PreDestroy
    public void preDestroy() {
        StartupTraceLog.record(StartupPhase.PRE_DESTROY, "@PreDestroy 执行。");
    }

    @Override
    public void destroy() {
        StartupTraceLog.record(StartupPhase.DISPOSABLE_BEAN, "DisposableBean.destroy 执行。");
    }

    public void customDestroy() {
        StartupTraceLog.record(StartupPhase.CUSTOM_DESTROY_METHOD, "自定义 destroy-method customDestroy 执行。");
    }
}
