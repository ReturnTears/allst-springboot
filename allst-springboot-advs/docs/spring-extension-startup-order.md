# Spring 扩展接口：按启动顺序回溯

当前工程是 **Spring Boot 2.3.5.RELEASE**。下面的顺序按 `SpringApplication.run` 和 `AbstractApplicationContext.refresh` 的真实调用关系整理，不是把接口名字按字母排出来的。

回溯时用两份材料对照：

- 这份说明：每个时机能做什么，以及本项目为什么这样用。
- 一次真实启动的轨迹：日志关键字 `[启动扩展点]`，或接口 `http://localhost:8030/advs/extension/trace`。

轨迹里的 `sequence` 是这次启动的真实先后。`phaseOrder` 是扩展点种类。不同 Bean 的实例化回调会交错，所以同一个阶段号会出现多次，顺序以 `sequence` 为准。

## 启动后怎么看

| 地址 | 内容 |
| --- | --- |
| `GET /advs/extension` | 摘要：banner、动态注册结果、导入模块、最近几条轨迹 |
| `GET /advs/extension/catalog` | 设计目录：时机、能做的事、本项目场景 |
| `GET /advs/extension/trace` | 这次启动实际走到的扩展点，并带上对应场景 |
| `GET /advs/extension/cache-points` | 从已有 Service 上扫到的缓存注解 |

`/extension/**` 的响应头是 `X-Advs-Extension: ready`。部门、员工、Druid 原来的路径没有挂这个拦截器。

可选启动参数：`--advs.job=extension-report`。它只在 `CommandLineRunner` 里多记一条轨迹。

## 哪些接口不能写成 @Component

这些回调发生在组件扫描之前，写在 `src/main/resources/META-INF/spring.factories`：

| SPI | 类 |
| --- | --- |
| `SpringApplicationRunListener` | `AdvsSpringApplicationRunListener` |
| `ApplicationListener`（早期事件和失败事件） | `AdvsEarlyApplicationListener` |
| `EnvironmentPostProcessor` | `AdvsEnvironmentPostProcessor` |
| `ApplicationContextInitializer` | `AdvsApplicationContextInitializer` |
| `FailureAnalyzer` | `AdvsStartupFailureAnalyzer` |

Boot 2.3 只认 `spring.factories`。2.7 之后的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 在这个工程里不会被读取。

`SpringApplicationRunListener` 还必须有公开构造器 `(SpringApplication application, String[] args)`。本实现的 `order` 是最高优先级，所以外层回调会先于 `EventPublishingRunListener` 发布的对应事件。Boot 自带那个监听器的顺序是 0。

`@EnableAdvsExtension` 加在启动类上。它不负责上面这五个 SPI，只负责配置类解析阶段的 `ImportSelector` 和 `ImportBeanDefinitionRegistrar`。

## 启动主路径

### 1. RunListener.starting / ApplicationStartingEvent

时机：`SpringApplication.run` 刚进入，还没有 Environment，也没有 Context。

能做的事：记启动起点、看原始参数。不能读 `application.yml` 的最终内容，也不能拿 Bean。

本项目：`AdvsSpringApplicationRunListener.starting` 记录参数个数。`AdvsEarlyApplicationListener` 接着记录 `ApplicationStartingEvent`。

### 2. environmentPrepared / ApplicationEnvironmentPreparedEvent

时机：Environment 已创建，系统属性和命令行已挂上。配置文件通常还没加载，加载发生在这个事件的监听器里。

能做的事：加一个占位 PropertySource。不能在 `environmentPrepared` 里判断端口或数据源“是不是没配”。

本项目：RunListener 只记录“配置还没加载”。早期监听器只记录事件已发布。

### 3. EnvironmentPostProcessor

时机：`ApplicationEnvironmentPreparedEvent` 之内。本实现 `order` 为最低，因此 `application.properties` 和 `application-beta.yml` 已经加载完，`@ConfigurationProperties` 还没绑定到 Bean。

能做的事：补默认配置、按环境改写配置、做启动前校验。用 `addLast`，让用户配置优先。

本项目：补齐这三项，且不覆盖已有值。

- `advs.extension.cache-warmup`，默认 `false`
- `advs.extension.fail-fast`，默认 `false`
- `advs.extension.banner`，默认 `ALLST Spring Extension Points`

`fail-fast=true` 且没有 `advs.extension.required-token` 时抛 `AdvsStartupException`。正常启动不要打开这个开关。

### 4. ApplicationContextInitializer

时机：Context 已创建，`refresh` 还没开始。

能做的事：注册早期单例、编程式加入后处理器。`registerSingleton` 放进去的对象不会再走 `BeanPostProcessor`。

本项目：注册 `advsStartupClock`，后面的接口用它算耗时。不改端口，不改数据源。

### 5. contextPrepared / ApplicationContextInitializedEvent / contextLoaded / ApplicationPreparedEvent

时机：初始化器之后，到 `refresh` 之前。

能做的事：确认早期单例还在，查看上下文类型和已经载入的 Bean 定义数量。不要 `getBean`。

本项目：确认时钟单例，记录上下文类名，然后标记即将刷新。

### 6. ImportSelector / ImportBeanDefinitionRegistrar

时机：`refresh` 内部，`ConfigurationClassPostProcessor` 解析 `@Configuration` 时。Bean 还没实例化。

能做的事：按注解把扫描包之外的模块引进来；按条件注册 Bean 定义。不要在 `selectImports` 里 new 业务服务并指望它被容器管理。

本项目：

- `AdvsExtensionImportSelector` 导入 `com.allst.advs.autoconfigure.AdvsImportedConfiguration`。这个包不在 `com.allst.springboot` 扫描范围内，拿掉 `@EnableAdvsExtension` 它就不会进容器。
- `AdvsExtensionRegistrar` 在 `advsDynamicNotice` 还没有定义时补注册。该类同时是 `@Component`，方便 IDE 识别注入，已有定义则不会再注册一次。

### 7. BeanDefinitionRegistryPostProcessor

时机：配置类解析之后，普通 `BeanFactoryPostProcessor` 之前。

能做的事：新增或删除 Bean 定义，统计当前定义。不要在这里 `getBean`。

本项目：注册 `advsFeatureSwitch`，并统计 `com.allst.springboot` 包下的定义数量。

这个接口同时继承了 `BeanFactoryPostProcessor`。第二个方法 `postProcessBeanFactory` 会在稍后的工厂后处理阶段再被调用。新增定义如果写在第二个方法里，可能来不及被其他后处理器看到。本项目的第二个方法只做确认。

### 8. BeanFactoryPostProcessor

时机：定义已经收集完，实例化还没开始。作为 Bean 的后处理器可以先拿到 `Environment`。

能做的事：改 scope、lazy-init、属性值。不要在这里注册新定义。

本项目：把 `banner` 和 `cache-warmup` 写进 `advsFeatureSwitch` 的属性。等它实例化时，这两个值已经在定义上。`/advs/extension` 返回的 banner 用来证明这条链路。

`BeanFactoryPostProcessor` 自身创建得太早，看不到后面才注册的 `BeanPostProcessor`。不要用实例化回调去包装后处理器自己。

### 9. WebServerFactoryCustomizer / ServletContextInitializer

时机：`refresh` 的 `onRefresh` 创建内嵌容器时，早于普通单例的批量初始化。定制器自己也是 Bean，会先被创建，然后才执行 `customize`。

能做的事：改端口、上下文路径、连接器，或追加 `ServletContextInitializer`。Boot 2.3 在创建 `TomcatWebServer` 时就会启动 Tomcat，所以 `ServletContextInitializer` 通常仍在 `onRefresh`。

本项目：不改 `server.port=8030`，不改 `context-path=/advs`。只往 `ServletContext` 写入 `advs.startup.initializedAt`。

### 10. 单个 Bean 的创建顺序

下面这条链是 **一个 Bean 内部** 的顺序。容器创建 DepartmentService、示例 Bean、Controller 时会互相交错。

跟踪对象主要是 `AdvsLifecycleDemoBean`。实例化回调如果作用到全部 Bean 上，很容易把 MyBatis 或 Druid 的创建过程打乱，所以只观察带 `@StartupParticipant` 的示例，或明确点名的类。

1. `InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation`  
   返回非空对象会跳过默认实例化。本项目返回 `null`。
2. `SmartInstantiationAwareBeanPostProcessor.determineCandidateConstructors`  
   只对组件扫描创建的 `AdvsConstructorSample` 记录。返回 `null`，构造器仍由容器选。  
   `@Bean` 工厂方法创建的 Bean **不会**进入这个回调。
3. 构造器。依赖还没注入。
4. `MergedBeanDefinitionPostProcessor.postProcessMergedBeanDefinition`  
   构造之后、填充属性之前。适合读注解，不适合改业务数据。  
   本项目扫描已有 Service 上的 `@CacheConfig`、`@Cacheable`、`@CacheEvict`、`@CachePut`、`@Caching`，结果在 `/advs/extension/cache-points`。部门缓存原来的注解没有改。
5. `getEarlyBeanReference`  
   只有循环依赖、Bean 被提前暴露时才会调用。本项目不制造循环依赖；一旦工程里的 Bean 走到这里，轨迹会记下来。
6. `postProcessAfterInstantiation`  
   必须返回 `true`。返回 `false` 会取消属性填充。
7. `postProcessProperties`  
   可以改写即将注入的属性。示例 Bean 原样返回。
8. `BeanNameAware`、`BeanClassLoaderAware`、`BeanFactoryAware`  
   这三个在任何 `BeanPostProcessor` 之前。此处不要 `getBean`。
9. `EnvironmentAware`、`ApplicationContextAware`  
   由 `ApplicationContextAwareProcessor` 在初始化前回调里触发，早于 `@PostConstruct`。这时可以读 Environment，但其他单例可能还没创建完。
10. `BeanPostProcessor.postProcessBeforeInitialization`  
    本项目的处理器顺序早于 `CommonAnnotationBeanPostProcessor`，所以先打点，再进 `@PostConstruct`。
11. `@PostConstruct`
12. `InitializingBean.afterPropertiesSet`
13. `@Bean(initMethod = "customInit")`  
    方法名故意不叫 `afterPropertiesSet`，避免和接口方法被看成同一次调用。
14. `BeanPostProcessor.postProcessAfterInitialization`  
    AOP 代理通常在这里包装。示例 Bean 原样返回，不替换已有缓存代理。

同一 Bean 上还有两个更晚或更早的相关点：

- `FactoryBean.getObject`：谁注入产品对象，谁触发创建。`AdvsExtensionCatalogFactoryBean` 生产 `ExtensionCatalog`。注入 `ExtensionCatalog` 拿到的是目录；工厂自身要加 `&`。
- `WebMvcConfigurer.addInterceptors`：`RequestMappingHandlerMapping` 初始化时调用，仍属于单例创建阶段。只注册 `/extension` 拦截器。

### 11. SmartInitializingSingleton

时机：所有非懒加载单例都已创建，`SmartLifecycle.start` 之前。

能做的事：跨 Bean 自检。多个实现之间没有稳定的业务顺序，不要用来编排“必须先 A 再 B”。

本项目：`AdvsSingletonReadyCallback` 统计缓存切点，并确认原来的 `DepartmentService`、`EmployeeMapper` 仍在容器里。示例 Bean 自己也会收到一次回调。

### 12. SmartLifecycle.start

时机：`finishRefresh` 中，`ContextRefreshedEvent` 之前。`phase` 越小越早启动。本实现用 `0`。

能做的事：启动会随应用关闭而停止的后台资源。

注意：Boot 2.3 的 Tomcat 往往在前面的 `onRefresh` 里已经启动。这里不能当成“端口刚刚打开”，更不能当成“可以接流量”。本项目只把扩展模块标记为运行中，不新开线程，避免影响已有的 `@Async` 和 `@Scheduled`。

### 13. ContextRefreshedEvent / RunListener.started / ApplicationStartedEvent

时机：刷新结束，Runner 还没跑。

能做的事：记录已启动耗时。依赖 Runner 结果的逻辑不要放这里。

本项目把 RunListener 和事件分开记，方便区分“一个 run 监听器”和“一组 ApplicationListener”。

`ApplicationStartedEvent` 的监听器返回之后，Boot 才发布存活状态 `LivenessState.CORRECT`，然后才跑 Runner。在 Started 事件里面读存活状态，读到的还是切换前的值。

### 14. ApplicationRunner / CommandLineRunner

时机：`ApplicationStartedEvent` 之后、`ApplicationReadyEvent` 之前。两者在 **同一个** `@Order` 队列里，不是所有 ApplicationRunner 都天然先于 CommandLineRunner。

本项目：

- `AdvsCacheWarmupRunner`，`@Order(10)`。默认跳过。`advs.extension.cache-warmup=true` 时只列出 `CacheManager`，读取失败记一条轨迹，不中断启动。
- `AdvsCommandJobRunner`，`@Order(20)`。没有 `--advs.job` 时保持空闲。

Runner 里抛出的异常会让整个启动失败，所以可选任务不要直接抛出去。

### 15. RunListener.running / ApplicationReadyEvent

时机：Runner 已结束。Boot 2.3 先发布 `ApplicationReadyEvent`，等这个事件的监听器都返回，再发布 `AvailabilityChangeEvent(ReadinessState.ACCEPTING_TRAFFIC)`。

能做的事：在 Ready 事件里做最后的启动通知。真正对外接流量，要等随后的就绪状态变更。

本项目：Ready 事件里读到的 readiness 是 `REFUSING_TRAFFIC`。下一条轨迹才是 `ACCEPTING_TRAFFIC`。判断“可以接请求”看这次状态变更，不看 `SmartLifecycle.start`，也不要只看 Ready 事件本身。

## 关闭路径

正常启动后的 `/extension/trace` 里还没有这些记录。进程退出、Context 关闭时才会出现在日志里。

关闭开始前，Boot 会先把就绪状态改回 `REFUSING_TRAFFIC`，然后才进入下面的销毁顺序。

1. `ContextClosedEvent`：Bean 都还在，适合做下线通知。
2. `SmartLifecycle.stop`：先停后台资源。
3. `DestructionAwareBeanPostProcessor.postProcessBeforeDestruction`：只对 `AdvsLifecycleDemoBean` 返回 `requiresDestruction=true`，避免把销毁回调扩到全部 Bean。
4. `@PreDestroy`
5. `DisposableBean.destroy`
6. `@Bean(destroyMethod = "customDestroy")`

销毁方法如果也叫 `destroy`，容器会和 `DisposableBean` 去重。所以自定义方法用了另一个名字。

## 失败路径

失败如果发生在 Context 刷新之前，Bean 形态的监听器还不存在，所以失败事件放在 `spring.factories` 的早期监听器里。

顺序是：

1. `SpringApplicationRunListener.failed`。Context 可能是 `null`。
2. `ApplicationFailedEvent`。
3. `FailureAnalyzer`。
4. 如果 Context 已经创建，接着才会 `close`，于是再走进关闭路径。

`AdvsStartupFailureAnalyzer` 只分析 `AdvsStartupException`。数据源、Redis、Elasticsearch 的失败仍走 Boot 原来的分析器。

## 和已有功能的边界

- 没有删除或改写部门、员工、缓存、Druid、邮件、异步和定时任务的业务代码。
- 缓存注解只读，不改 `DepartmentServiceImpl` 的 key 和条件。
- 默认不在启动时访问 Redis。
- `fail-fast` 默认关闭。
- Web 定制不改已经在用的端口和上下文路径。

## 源码位置

```text
com.allst.springboot.extension.bootstrap      容器创建前的 SPI
com.allst.springboot.extension.imports        @EnableAdvsExtension
com.allst.advs.autoconfigure                  扫描包之外、靠 ImportSelector 进入的配置
com.allst.springboot.extension.processor      Bean 定义、工厂和实例化后处理器
com.allst.springboot.extension.lifecycle      Aware、初始化、Runner、Lifecycle
com.allst.springboot.extension.web            Web 容器和 MVC 扩展
com.allst.springboot.extension.catalog        回溯目录
com.allst.springboot.controller.ExtensionTraceController
```

阶段号定义在 `StartupPhase`，说明文字在 `ExtensionCatalog`。改场景时两处和这份文档一起改，避免以后对不上。
