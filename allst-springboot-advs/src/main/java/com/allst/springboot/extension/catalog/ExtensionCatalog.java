package com.allst.springboot.extension.catalog;

import com.allst.springboot.extension.StartupPhase;
import com.allst.springboot.extension.model.ExtensionPointDoc;
import com.allst.springboot.extension.model.StartupRecord;
import com.allst.springboot.extension.model.StartupTraceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按启动顺序整理的扩展点目录，是接口回溯数据的来源。
 */
public class ExtensionCatalog {

    public static final String SPRING_BOOT_VERSION = "2.3.5.RELEASE";

    private final List<ExtensionPointDoc> points;
    private final Map<Integer, ExtensionPointDoc> byOrder;

    public ExtensionCatalog(List<ExtensionPointDoc> points) {
        this.points = List.copyOf(points);
        Map<Integer, ExtensionPointDoc> index = new LinkedHashMap<>();
        for (ExtensionPointDoc point : this.points) {
            index.put(point.getOrder(), point);
        }
        this.byOrder = Collections.unmodifiableMap(index);
    }

    public static ExtensionCatalog create() {
        return new ExtensionCatalog(documentedPoints());
    }

    public List<ExtensionPointDoc> getPoints() {
        return points;
    }

    public String getSpringBootVersion() {
        return SPRING_BOOT_VERSION;
    }

    public List<StartupTraceView> explain(List<StartupRecord> records) {
        List<StartupTraceView> views = new ArrayList<>(records.size());
        for (StartupRecord record : records) {
            ExtensionPointDoc doc = byOrder.get(record.getPhaseOrder());
            views.add(new StartupTraceView(
                    record.getSequence(),
                    record.getPhaseOrder(),
                    record.getPhase(),
                    record.getExtensionPoint(),
                    record.getAction(),
                    doc == null ? "" : doc.getProjectScenario(),
                    doc == null ? "" : doc.getImplementation(),
                    record.getTimestampMillis()));
        }
        return views;
    }

    private static List<ExtensionPointDoc> documentedPoints() {
        List<ExtensionPointDoc> docs = new ArrayList<>();
        docs.add(doc(StartupPhase.RUN_LISTENER_STARTING, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "META-INF/spring.factories，不能使用 @Component",
                "SpringApplication.run 刚进入，Environment 和 Context 都还没有。监听器需要 (SpringApplication, String[]) 构造器。本实现 order 为最高优先级，外层回调先于对应事件。",
                "统计启动耗时、校验启动参数、注册最早期的状态。不要读 application.yml 的最终值，也不要碰 Bean。",
                "记录 main 参数个数，并作为整次启动耗时的起点。"));
        docs.add(doc(StartupPhase.EVENT_STARTING, "启动主路径",
                "AdvsEarlyApplicationListener",
                "META-INF/spring.factories 的 ApplicationListener",
                "RunListener.starting 发布 ApplicationStartingEvent。此时日志系统可能还没完成初始化。",
                "多个监听器并行关心“应用开始了”。适合打点，不适合依赖日志配置或配置文件。",
                "只记录事件已发布。后续环境加工交给 EnvironmentPostProcessor。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_ENVIRONMENT_PREPARED, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "Environment 对象已经创建，系统属性和命令行已挂上，但 application.properties / yml 通常还没加载。",
                "可以挂上额外的 PropertySource 占位。不要在这里判断 server.port、数据源是否配置完整。",
                "明确跳过业务配置读取，避免把“配置尚未加载”误判成“配置缺失”。"));
        docs.add(doc(StartupPhase.EVENT_ENVIRONMENT_PREPARED, "启动主路径",
                "AdvsEarlyApplicationListener",
                "spring.factories",
                "ApplicationEnvironmentPreparedEvent 发布。Boot 会在这个事件里加载配置文件并调用 EnvironmentPostProcessor。",
                "监听环境就绪。若监听器顺序晚于配置加载器，读到的就是最终 Environment。",
                "本监听器优先级最高，只标记事件发布，让后面的 EnvironmentPostProcessor 再读最终配置。"));
        docs.add(doc(StartupPhase.ENVIRONMENT_POST_PROCESSOR, "启动主路径",
                "AdvsEnvironmentPostProcessor",
                "spring.factories 的 EnvironmentPostProcessor，order 放在最后",
                "配置文件已经加载。Bean 定义尚未解析，@ConfigurationProperties 也还没绑定。",
                "补默认配置、按环境改写 PropertySource、做 fail-fast 校验。用 addLast 或先判断 contains，避免覆盖用户配置。",
                "补齐 advs.extension.cache-warmup、fail-fast、banner。fail-fast=true 且缺少 required-token 时抛出 AdvsStartupException。"));
        docs.add(doc(StartupPhase.CONTEXT_INITIALIZER, "启动主路径",
                "AdvsApplicationContextInitializer",
                "spring.factories 的 ApplicationContextInitializer",
                "Context 已创建并完成 setEnvironment，refresh 还没开始。",
                "注册早期单例、打开或关闭某些上下文能力、编程式添加 BeanFactoryPostProcessor。这里 registerSingleton 的对象不会再走 BeanPostProcessor。",
                "注册 advsStartupClock，供后续接口计算启动耗时。不修改端口和已有数据源。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_CONTEXT_PREPARED, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "ApplicationContextInitializer 已经全部执行完。",
                "确认初始化器留下的早期对象是否存在。",
                "检查 advsStartupClock 是否已作为单例放进 BeanFactory。"));
        docs.add(doc(StartupPhase.EVENT_CONTEXT_INITIALIZED, "启动主路径",
                "AdvsEarlyApplicationListener",
                "spring.factories",
                "ApplicationContextInitializedEvent。主配置类尚未注册成 Bean 定义。",
                "读取上下文类型，区分 Servlet 容器应用和普通应用。",
                "记录当前上下文实现类，确认走的是 Web 应用上下文。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_CONTEXT_LOADED, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "启动类等 sources 已载入 Context，refresh 尚未调用。",
                "最后一次在刷新前观察 BeanFactory。不要触发 getBean。",
                "记录刷新前的准备完成。"));
        docs.add(doc(StartupPhase.EVENT_PREPARED, "启动主路径",
                "AdvsEarlyApplicationListener",
                "spring.factories",
                "ApplicationPreparedEvent。下一步就是 AbstractApplicationContext.refresh。",
                "刷新前的最后事件。失败分析器还没介入。",
                "标记即将进入 refresh。"));
        docs.add(doc(StartupPhase.IMPORT_SELECTOR, "启动主路径",
                "AdvsExtensionImportSelector",
                "@EnableAdvsExtension 上的 @Import，发生在 ConfigurationClassPostProcessor 解析配置类时",
                "refresh 内部、Bean 实例化之前。适合装载不在启动类扫描包里的模块。",
                "按注解属性决定导入哪些 @Configuration。不要在 selectImports 里创建业务 Bean。",
                "导入扫描范围外的 com.allst.advs.autoconfigure.AdvsImportedConfiguration。"));
        docs.add(doc(StartupPhase.IMPORT_REGISTRAR, "启动主路径",
                "AdvsExtensionRegistrar",
                "@EnableAdvsExtension 上的 @Import",
                "配置类解析完成后、普通 BeanFactoryPostProcessor 之前。",
                "按条件向注册器写入 BeanDefinition，适合“有开关才装配”的模块。",
                "注册没有 @Component 的 advsDynamicNotice，避免被组件扫描重复注册。"));
        docs.add(doc(StartupPhase.BEAN_DEFINITION_REGISTRY, "启动主路径",
                "AdvsBeanDefinitionRegistryPostProcessor",
                "@Component。它是 BeanDefinitionRegistryPostProcessor，会在配置类解析之后被回调",
                "可以新增或删除 Bean 定义。这是批量改定义的正确位置。",
                "按配置动态注册 Bean、统计定义数量。不要在这里 getBean。",
                "注册 advsFeatureSwitch，并统计 com.allst.springboot 包下已有的 Bean 定义数量。"));
        docs.add(doc(StartupPhase.REGISTRY_AS_FACTORY_POST, "启动主路径",
                "AdvsBeanDefinitionRegistryPostProcessor",
                "同一个注册器的第二个方法，属于随后的工厂后处理阶段",
                "BeanDefinitionRegistryPostProcessor 继承了 BeanFactoryPostProcessor，第二个方法会再被调用一次。",
                "可以继续修改已有定义。新增定义要放回 postProcessBeanDefinitionRegistry，否则可能来不及被后处理器看到。",
                "只记录这个二次回调，真正改属性放到独立的 BeanFactoryPostProcessor。"));
        docs.add(doc(StartupPhase.BEAN_FACTORY_POST_PROCESSOR, "启动主路径",
                "AdvsBeanFactoryPostProcessor",
                "@Component",
                "全部 Bean 定义已收集完，实例化还没开始。可以注入 Environment。",
                "修改 scope、lazy-init、属性值、替换占位符。不要注册新的 Bean 定义。",
                "把 banner 和 cache-warmup 写入 advsFeatureSwitch 的属性值，等它稍后实例化时生效。"));
        docs.add(doc(StartupPhase.WEB_SERVER_CUSTOMIZER, "启动主路径",
                "AdvsWebServerFactoryCustomizer",
                "@Component。在 refresh.onRefresh 创建内嵌容器、初始化 WebServerFactory 时调用",
                "早于普通单例的批量初始化。定制器本身也是 Bean，会先走自己的实例化回调。",
                "改端口、上下文路径、连接器、附加 ServletContextInitializer。本项目不改已有端口 8030 和 context-path /advs。",
                "只追加一个 ServletContext 初始化回调，把启动信息放进 ServletContext。"));
        docs.add(doc(StartupPhase.SERVLET_CONTEXT_INITIALIZER, "启动主路径",
                "AdvsWebServerFactoryCustomizer 内注册的 ServletContextInitializer",
                "WebServerFactory.addInitializers",
                "Boot 2.3 在创建 TomcatWebServer 时就会启动容器，因此这个回调通常仍在 onRefresh，早于大部分业务单例。",
                "注册 Servlet、Filter、Listener，或写入 ServletContext 属性。Filter 若要拦截已有接口，不要在这里改 Druid 的过滤规则。",
                "写入 advs.startup.initializedAt，供后续 Filter 读取。不注册新的业务 Filter。"));
        docs.add(doc(StartupPhase.BEFORE_INSTANTIATION, "启动主路径",
                "AdvsInstantiationPostProcessor",
                "@Component，只观察带 @StartupParticipant 的示例 Bean",
                "每个 Bean 真正构造之前。返回非空对象会跳过容器默认实例化。",
                "可以返回代理从而短路创建。返回 null 表示继续默认流程。",
                "对 AdvsLifecycleDemoBean 只记录时机并返回 null，避免影响已有 Service 的创建。"));
        docs.add(doc(StartupPhase.DETERMINE_CONSTRUCTOR, "启动主路径",
                "AdvsSmartInstantiationPostProcessor",
                "@Component",
                "决定用哪个构造器。发生在实例化前短路之后、构造器执行之前。",
                "为有多个构造器的类指定入口。返回 null 表示仍由 Spring 推断。",
                "示例 Bean 返回 null，沿用无参构造器。不干预部门、员工相关 Bean。"));
        docs.add(doc(StartupPhase.CONSTRUCTOR, "启动主路径",
                "AdvsLifecycleDemoBean 构造器",
                "容器实例化行为，不是扩展接口",
                "构造器执行时依赖注入尚未发生，Aware 也还没回调。",
                "只做轻量赋值。不要在构造器里访问还没创建的 Bean。",
                "用这条记录把“推断构造器”和“合并 Bean 定义”隔开，方便对照源码顺序。"));
        docs.add(doc(StartupPhase.MERGED_BEAN_DEFINITION, "启动主路径",
                "AdvsMergedBeanDefinitionPostProcessor",
                "@Component",
                "构造完成之后、属性填充之前。AutowiredAnnotationBeanPostProcessor 也在这里缓存注入点。",
                "读取合并后的 Bean 定义和注解元数据，不适合改业务状态。",
                "扫描已有 Service 上的 @Cacheable、@CacheEvict、@CachePut、@Caching、@CacheConfig，汇总部门缓存切点。"));
        docs.add(doc(StartupPhase.EARLY_REFERENCE, "启动主路径",
                "AdvsSmartInstantiationPostProcessor",
                "@Component",
                "仅当出现循环依赖、Bean 被提前暴露时才会调用。",
                "在早期引用外再包一层代理，保证循环依赖双方拿到的是最终代理。",
                "本项目不制造循环依赖。方法保留：一旦工程 Bean 被提前暴露，会留下记录。"));
        docs.add(doc(StartupPhase.AFTER_INSTANTIATION, "启动主路径",
                "AdvsInstantiationPostProcessor",
                "@Component",
                "实例已在，属性尚未填充。",
                "返回 false 会取消属性填充。正常场景必须返回 true。",
                "示例 Bean 返回 true，只记录“属性填充即将开始”。"));
        docs.add(doc(StartupPhase.PROPERTY_POST_PROCESS, "启动主路径",
                "AdvsInstantiationPostProcessor",
                "@Component",
                "属性填充前的最后加工，@Autowired 的解析也在相近位置。",
                "可以改写 PropertyValues。返回值必须是可用的属性集合，不能把已有 Bean 的依赖清空。",
                "示例 Bean 原样返回属性，不做改写。"));
        docs.add(doc(StartupPhase.AWARE_BEAN_NAME, "启动主路径",
                "AdvsLifecycleDemoBean",
                "@Bean(initMethod, destroyMethod)",
                "initializeBean 的第一步，早于任何 BeanPostProcessor。",
                "拿到容器中的 beanName。适合按名字做日志或注册表键。",
                "记录示例 Bean 的真实 bean 名。"));
        docs.add(doc(StartupPhase.AWARE_CLASS_LOADER, "启动主路径",
                "AdvsLifecycleDemoBean",
                "Bean 回调",
                "紧跟 BeanNameAware。",
                "按类加载器加载额外资源。不要在这里启动线程。",
                "记录类加载器名称，确认应用类加载器已可用。"));
        docs.add(doc(StartupPhase.AWARE_BEAN_FACTORY, "启动主路径",
                "AdvsLifecycleDemoBean",
                "Bean 回调",
                "仍早于 BeanPostProcessor。其他单例可能还没创建完。",
                "可以查看 Bean 定义。不要在这里 getBean 去触发一串未完成的依赖。",
                "只确认 BeanFactory 已注入，不做额外查找。"));
        docs.add(doc(StartupPhase.AWARE_ENVIRONMENT, "启动主路径",
                "AdvsLifecycleDemoBean",
                "由 ApplicationContextAwareProcessor 在 postProcessBeforeInitialization 里回调",
                "位于 BeanFactoryAware 之后、@PostConstruct 之前。",
                "读取已经加工完成的 Environment。配置绑定、profile 判断放这里是安全的。",
                "读取 server.port，确认 beta 配置已经进入 Environment。"));
        docs.add(doc(StartupPhase.AWARE_APPLICATION_CONTEXT, "启动主路径",
                "AdvsLifecycleDemoBean",
                "ApplicationContextAwareProcessor",
                "和 EnvironmentAware 同一处理器内，晚于 EnvironmentAware。",
                "可以按类型查找 Bean，但全量自检要等 SmartInitializingSingleton 或 ApplicationRunner。",
                "只确认 advsStartupClock 已经存在，不提前初始化部门或员工 Service。"));
        docs.add(doc(StartupPhase.BEAN_POST_BEFORE_INIT, "启动主路径",
                "AdvsBeanPostProcessor",
                "@Component，PriorityOrdered，顺序早于 CommonAnnotationBeanPostProcessor",
                "初始化前回调。@PostConstruct 也是由另一个 BeanPostProcessor 在这个阶段调用的。",
                "校验依赖、包装返回值。返回 null 会中断后续初始化。",
                "示例 Bean 在 @PostConstruct 之前打点，证明自定义处理器可以排在注解初始化之前。"));
        docs.add(doc(StartupPhase.POST_CONSTRUCT, "启动主路径",
                "AdvsLifecycleDemoBean.postConstruct",
                "javax.annotation.PostConstruct",
                "初始化前 BeanPostProcessor 链内部。早于 InitializingBean。",
                "依赖已注入，适合建立本 Bean 自己的状态。不要依赖其他 Bean 的 @PostConstruct 已执行。",
                "示例 Bean 标记自身初始化开始。"));
        docs.add(doc(StartupPhase.INITIALIZING_BEAN, "启动主路径",
                "AdvsLifecycleDemoBean",
                "InitializingBean",
                "@PostConstruct 之后、自定义 init-method 之前。",
                "afterPropertiesSet 适合检查属性是否齐全。",
                "确认示例 Bean 的 Aware 回调已经发生。"));
        docs.add(doc(StartupPhase.CUSTOM_INIT_METHOD, "启动主路径",
                "AdvsLifecycleConfiguration 的 initMethod=customInit",
                "@Bean(initMethod = \"customInit\")",
                "InitializingBean 之后、postProcessAfterInitialization 之前。",
                "不想实现 Spring 接口时，用方法名挂初始化逻辑。方法名不要和 afterPropertiesSet 重复。",
                "示例 Bean 用独立方法 customInit，避免和 InitializingBean 合成一次调用。"));
        docs.add(doc(StartupPhase.BEAN_POST_AFTER_INIT, "启动主路径",
                "AdvsBeanPostProcessor",
                "@Component",
                "初始化方法全部结束后。AOP 代理通常在这个时机包上。",
                "返回包装对象就会替换容器中的 Bean。本项目不替换，避免影响已有缓存代理。",
                "示例 Bean 原样返回，并说明这里是代理包装点。"));
        docs.add(doc(StartupPhase.FACTORY_BEAN, "启动主路径",
                "AdvsExtensionCatalogFactoryBean",
                "@Component。注入 ExtensionCatalog 时触发 getObject，不是注入 FactoryBean 本身",
                "产品对象在被依赖时创建，一般落在单例初始化阶段，早于 SmartInitializingSingleton。",
                "复杂对象、需要缓存实例或隐藏构造细节时使用。调用方只依赖产品类型。",
                "组装这份扩展点目录。&beanName 才是 FactoryBean 自身。"));
        docs.add(doc(StartupPhase.WEB_MVC_CONFIGURER, "启动主路径",
                "AdvsWebMvcExtension",
                "@Configuration 实现 WebMvcConfigurer，组件扫描自动生效",
                "RequestMappingHandlerMapping 初始化时回调，仍在单例创建阶段。",
                "加拦截器、格式化、跨域、参数解析。拦截范围不要覆盖已有 /dept、/druid 接口。",
                "只给 /extension/** 加拦截器，响应头 X-Advs-Extension=ready。"));
        docs.add(doc(StartupPhase.SMART_INITIALIZING_SINGLETON, "启动主路径",
                "AdvsSingletonReadyCallback、AdvsLifecycleDemoBean",
                "@Component 与示例 Bean 各自实现",
                "所有非懒加载单例创建完毕，SmartLifecycle.start 之前。",
                "跨 Bean 自检、缓存预热登记、事件监听器方法的最终注册。同一阶段内多个回调没有稳定的业务顺序。",
                "统计缓存切点和轨迹条数，并确认 DepartmentService、EmployeeMapper 等已有 Bean 仍在容器中。"));
        docs.add(doc(StartupPhase.LIFECYCLE_START, "启动主路径",
                "AdvsSmartLifecycle",
                "@Component，phase = 0",
                "finishRefresh 里、ContextRefreshedEvent 之前。phase 越小越早 start。",
                "启动随应用关闭而停止的后台资源。Boot 2.3 的 Tomcat 在 onRefresh 里往往已经启动，所以这里不能当成“端口刚刚打开”。对外可接流量要以 ApplicationReadyEvent 为准。",
                "只把扩展模块标记为 running，不新开线程，避免干扰已有 @Async 和 @Scheduled。"));
        docs.add(doc(StartupPhase.EVENT_CONTEXT_REFRESHED, "启动主路径",
                "AdvsContextEventListener",
                "@Component",
                "refresh 结束时发布。Runner 还没执行。",
                "父容器刷新完成通知。有子容器时可能收到多次，处理逻辑要可重复。",
                "记录上下文刷新完成，作为 Runner 之前的分界。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_STARTED, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "refresh 已结束，ApplicationRunner 和 CommandLineRunner 还没调用。",
                "记录已启动耗时。需要依赖 Runner 结果的逻辑不要放这里。",
                "输出从 starting 到此刻的毫秒数。"));
        docs.add(doc(StartupPhase.EVENT_STARTED, "启动主路径",
                "AdvsContextEventListener",
                "@Component，监听 ApplicationStartedEvent",
                "与 RunListener.started 同一时段，由 EventPublishingRunListener 发布。",
                "多个业务模块可以各自听这个事件。",
                "和 RunListener.started 分开记录，便于区分“一个监听器”和“一组事件监听器”。"));
        docs.add(doc(StartupPhase.APPLICATION_RUNNER, "启动主路径",
                "AdvsCacheWarmupRunner",
                "@Component，@Order(10)",
                "ApplicationStartedEvent 之后。和 CommandLineRunner 放在同一个有序列表里，不是固定先于所有 CommandLineRunner。",
                "应用已能安全调用 Bean，适合预热、打印清单、一次性校验。异常会让启动失败。",
                "默认不预热缓存。advs.extension.cache-warmup=true 时只列出 CacheManager 名称，失败只记录，不中断启动。"));
        docs.add(doc(StartupPhase.COMMAND_LINE_RUNNER, "启动主路径",
                "AdvsCommandJobRunner",
                "@Component，@Order(20)",
                "与 ApplicationRunner 按 @Order 混合排序。参数来自 ApplicationArguments。",
                "解析 --key=value 形式的一次性任务。未知任务应忽略或记录，不要让可选参数拖垮启动。",
                "没有 --advs.job 时保持空闲。值为 extension-report 时记录当前轨迹条数。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_RUNNING, "启动主路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "全部 Runner 已结束，ApplicationReadyEvent 即将发布。",
                "启动成功的外层通知。",
                "记录总耗时，表示 run 方法即将返回。"));
        docs.add(doc(StartupPhase.EVENT_READY, "启动主路径",
                "AdvsContextEventListener",
                "@Component，监听 ApplicationReadyEvent",
                "应用就绪。Boot 2.3 会把 Readiness 切到 ACCEPTING_TRAFFIC。",
                "对外流量、注册发现、就绪探针放这里，而不是放在 SmartLifecycle.start。",
                "读取 ApplicationAvailability 的存活和就绪状态，写入轨迹。"));
        docs.add(doc(StartupPhase.EVENT_CONTEXT_CLOSED, "关闭路径",
                "AdvsContextEventListener",
                "@Component，监听 ContextClosedEvent",
                "context.close 开始时。Bean 还没有销毁，SmartLifecycle.stop 也还没调用。",
                "发布下线通知、拒绝新请求。此阶段仍可以使用其他 Bean。",
                "记录关闭开始。正常启动的 /extension/trace 里看不到它，进程退出时才会出现在日志中。"));
        docs.add(doc(StartupPhase.LIFECYCLE_STOP, "关闭路径",
                "AdvsSmartLifecycle",
                "LifecycleProcessor.onClose",
                "ContextClosedEvent 之后、destroyBeans 之前。phase 越大越早 stop。",
                "先停后台资源，再销毁 Bean。",
                "把扩展模块标记回未运行。不触碰已有定时任务的调度器。"));
        docs.add(doc(StartupPhase.BEFORE_DESTRUCTION, "关闭路径",
                "AdvsDestructionPostProcessor",
                "DestructionAwareBeanPostProcessor，PriorityOrdered，早于 CommonAnnotationBeanPostProcessor",
                "单个 Bean 销毁前。只对该 Bean 返回 requiresDestruction=true，避免把销毁回调扩到全部 Bean。",
                "释放该 Bean 持有的外部资源之前做标记。",
                "只跟踪 AdvsLifecycleDemoBean，并排在 @PreDestroy 之前。"));
        docs.add(doc(StartupPhase.PRE_DESTROY, "关闭路径",
                "AdvsLifecycleDemoBean.preDestroy",
                "@PreDestroy",
                "由 CommonAnnotationBeanPostProcessor 在 postProcessBeforeDestruction 中调用。早于 DisposableBean。",
                "释放本 Bean 资源。",
                "示例 Bean 按容器真实销毁顺序留下记录。"));
        docs.add(doc(StartupPhase.DISPOSABLE_BEAN, "关闭路径",
                "AdvsLifecycleDemoBean",
                "DisposableBean",
                "@PreDestroy 之后、自定义 destroy-method 之前。",
                "destroy 里做 Spring 风格的资源释放。方法名若与 destroy-method 相同，容器会去重。",
                "使用 destroy()，自定义方法另起名 customDestroy。"));
        docs.add(doc(StartupPhase.CUSTOM_DESTROY_METHOD, "关闭路径",
                "AdvsLifecycleConfiguration 的 destroyMethod=customDestroy",
                "@Bean(destroyMethod = \"customDestroy\")",
                "该 Bean 销毁链的最后一步。",
                "非 Spring 接口的清理方法。",
                "示例 Bean 用独立方法观察三次销毁回调的顺序。"));
        docs.add(doc(StartupPhase.RUN_LISTENER_FAILED, "失败路径",
                "AdvsSpringApplicationRunListener",
                "spring.factories",
                "run 抛错后、FailureAnalyzer 之前。Context 可能为 null，例如失败发生在 refresh 之前。",
                "记录失败点。不要假设 Bean 已经创建。",
                "记录异常类型和 Context 是否存在。"));
        docs.add(doc(StartupPhase.EVENT_FAILED, "失败路径",
                "AdvsEarlyApplicationListener",
                "spring.factories，才能在 Context 创建失败时仍收到事件",
                "ApplicationFailedEvent。Bean 形态的监听器此时可能还不存在。",
                "失败告警。",
                "早期监听器记录失败事件，不依赖容器里的 Bean。"));
        docs.add(doc(StartupPhase.FAILURE_ANALYZER, "失败路径",
                "AdvsStartupFailureAnalyzer",
                "spring.factories 的 FailureAnalyzer",
                "启动失败报告阶段。只处理自己声明的异常类型，避免盖住数据源、Redis 的原有分析。",
                "把异常翻译成可执行的排查说明。",
                "只分析 AdvsStartupException。打开 fail-fast 且缺少 required-token 时会走到这里。"));
        return List.copyOf(docs);
    }

    private static ExtensionPointDoc doc(StartupPhase phase, String pathKind, String implementation,
                                          String registration, String timing, String capabilities,
                                          String projectScenario) {
        return new ExtensionPointDoc(
                phase.getOrder(),
                pathKind,
                phase.getTitle(),
                phase.getExtensionPoint(),
                implementation,
                registration,
                timing,
                capabilities,
                projectScenario);
    }
}
