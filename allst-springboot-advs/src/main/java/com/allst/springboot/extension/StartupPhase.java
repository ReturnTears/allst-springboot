package com.allst.springboot.extension;

/**
 * Spring Boot 2.3.5 启动过程中的扩展点槽位。
 * <p>
 * order 用来把同一种扩展点归到同一阶段；一次真实启动里的先后以 {@link StartupTraceLog} 的 sequence 为准。
 * 不同 Bean 的实例化回调会交错，阶段号不是全局屏障。
 */
public enum StartupPhase {

    RUN_LISTENER_STARTING(10, "RunListener.starting", "SpringApplicationRunListener"),
    EVENT_STARTING(20, "ApplicationStartingEvent", "ApplicationListener"),
    RUN_LISTENER_ENVIRONMENT_PREPARED(30, "RunListener.environmentPrepared", "SpringApplicationRunListener"),
    EVENT_ENVIRONMENT_PREPARED(40, "ApplicationEnvironmentPreparedEvent", "ApplicationListener"),
    ENVIRONMENT_POST_PROCESSOR(50, "EnvironmentPostProcessor", "EnvironmentPostProcessor"),
    CONTEXT_INITIALIZER(60, "ApplicationContextInitializer", "ApplicationContextInitializer"),
    RUN_LISTENER_CONTEXT_PREPARED(70, "RunListener.contextPrepared", "SpringApplicationRunListener"),
    EVENT_CONTEXT_INITIALIZED(80, "ApplicationContextInitializedEvent", "ApplicationListener"),
    RUN_LISTENER_CONTEXT_LOADED(90, "RunListener.contextLoaded", "SpringApplicationRunListener"),
    EVENT_PREPARED(100, "ApplicationPreparedEvent", "ApplicationListener"),
    IMPORT_SELECTOR(110, "ImportSelector", "ImportSelector"),
    IMPORT_REGISTRAR(120, "ImportBeanDefinitionRegistrar", "ImportBeanDefinitionRegistrar"),
    BEAN_DEFINITION_REGISTRY(130, "BeanDefinitionRegistryPostProcessor", "BeanDefinitionRegistryPostProcessor"),
    REGISTRY_AS_FACTORY_POST(140, "Registry.postProcessBeanFactory", "BeanFactoryPostProcessor"),
    BEAN_FACTORY_POST_PROCESSOR(150, "BeanFactoryPostProcessor", "BeanFactoryPostProcessor"),
    WEB_SERVER_CUSTOMIZER(160, "WebServerFactoryCustomizer", "WebServerFactoryCustomizer"),
    SERVLET_CONTEXT_INITIALIZER(170, "ServletContextInitializer", "ServletContextInitializer"),
    BEFORE_INSTANTIATION(180, "postProcessBeforeInstantiation", "InstantiationAwareBeanPostProcessor"),
    DETERMINE_CONSTRUCTOR(190, "determineCandidateConstructors", "SmartInstantiationAwareBeanPostProcessor"),
    CONSTRUCTOR(195, "构造器", "容器实例化"),
    MERGED_BEAN_DEFINITION(200, "postProcessMergedBeanDefinition", "MergedBeanDefinitionPostProcessor"),
    EARLY_REFERENCE(210, "getEarlyBeanReference", "SmartInstantiationAwareBeanPostProcessor"),
    AFTER_INSTANTIATION(220, "postProcessAfterInstantiation", "InstantiationAwareBeanPostProcessor"),
    PROPERTY_POST_PROCESS(230, "postProcessProperties", "InstantiationAwareBeanPostProcessor"),
    AWARE_BEAN_NAME(240, "BeanNameAware", "BeanNameAware"),
    AWARE_CLASS_LOADER(250, "BeanClassLoaderAware", "BeanClassLoaderAware"),
    AWARE_BEAN_FACTORY(260, "BeanFactoryAware", "BeanFactoryAware"),
    AWARE_ENVIRONMENT(270, "EnvironmentAware", "EnvironmentAware"),
    AWARE_APPLICATION_CONTEXT(280, "ApplicationContextAware", "ApplicationContextAware"),
    BEAN_POST_BEFORE_INIT(290, "postProcessBeforeInitialization", "BeanPostProcessor"),
    POST_CONSTRUCT(300, "@PostConstruct", "CommonAnnotationBeanPostProcessor"),
    INITIALIZING_BEAN(310, "InitializingBean.afterPropertiesSet", "InitializingBean"),
    CUSTOM_INIT_METHOD(320, "init-method", "@Bean(initMethod)"),
    BEAN_POST_AFTER_INIT(330, "postProcessAfterInitialization", "BeanPostProcessor"),
    FACTORY_BEAN(340, "FactoryBean.getObject", "FactoryBean"),
    WEB_MVC_CONFIGURER(350, "WebMvcConfigurer.addInterceptors", "WebMvcConfigurer"),
    SMART_INITIALIZING_SINGLETON(360, "SmartInitializingSingleton", "SmartInitializingSingleton"),
    LIFECYCLE_START(370, "SmartLifecycle.start", "SmartLifecycle"),
    EVENT_CONTEXT_REFRESHED(380, "ContextRefreshedEvent", "ApplicationListener"),
    RUN_LISTENER_STARTED(390, "RunListener.started", "SpringApplicationRunListener"),
    EVENT_STARTED(400, "ApplicationStartedEvent", "ApplicationListener"),
    APPLICATION_RUNNER(410, "ApplicationRunner", "ApplicationRunner"),
    COMMAND_LINE_RUNNER(420, "CommandLineRunner", "CommandLineRunner"),
    RUN_LISTENER_RUNNING(430, "RunListener.running", "SpringApplicationRunListener"),
    EVENT_READY(440, "ApplicationReadyEvent", "ApplicationListener"),
    EVENT_CONTEXT_CLOSED(450, "ContextClosedEvent", "ApplicationListener"),
    LIFECYCLE_STOP(460, "SmartLifecycle.stop", "SmartLifecycle"),
    BEFORE_DESTRUCTION(470, "postProcessBeforeDestruction", "DestructionAwareBeanPostProcessor"),
    PRE_DESTROY(480, "@PreDestroy", "CommonAnnotationBeanPostProcessor"),
    DISPOSABLE_BEAN(490, "DisposableBean.destroy", "DisposableBean"),
    CUSTOM_DESTROY_METHOD(500, "destroy-method", "@Bean(destroyMethod)"),
    RUN_LISTENER_FAILED(510, "RunListener.failed", "SpringApplicationRunListener"),
    EVENT_FAILED(520, "ApplicationFailedEvent", "ApplicationListener"),
    FAILURE_ANALYZER(530, "FailureAnalyzer", "FailureAnalyzer");

    private final int order;
    private final String title;
    private final String extensionPoint;

    StartupPhase(int order, String title, String extensionPoint) {
        this.order = order;
        this.title = title;
        this.extensionPoint = extensionPoint;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public String getExtensionPoint() {
        return extensionPoint;
    }
}
