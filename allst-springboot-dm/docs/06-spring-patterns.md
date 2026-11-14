# Spring 已经替你实现的模式

在 Spring Boot 里做设计，第一反应不该是「我要用哪个模式」，而是「**框架是不是已经做了**」。手写单例、手写工厂、手写代理，往往是没用上容器的结果。

## 对照表

| 你想做的事 | GoF 名字 | Spring / 开源框架里的现成实现 |
| --- | --- | --- |
| 全局一份、到处可用 | 单例 | `@Component` 默认 singleton 作用域；配置类、`@Value` |
| 按名字选实现 | 工厂方法 / 策略 | IoC 容器 + `Map<String, X>` 注入、`@Qualifier` |
| 一套组件配套切换 | 抽象工厂 | `@Profile` / `@ConditionalOnProperty` + 一组 `@Bean` |
| 每次取新对象 | 原型 | `@Scope("prototype")`、`ObjectProvider` |
| 不改类加横切逻辑 | 代理 / 装饰器 | AOP：`@Transactional`、`@Cacheable`、`@Async`、`@Around` |
| 固定流程 + 变化一步 | 模板方法 | `JdbcTemplate`、`RestTemplate`、`TransactionTemplate` 的回调 |
| 兼容不相同的接口 | 适配器 | MVC `HandlerAdapter`、Security provider、`MessageConverter` |
| 成功后通知多方 | 观察者 | `ApplicationEvent` / `@EventListener` / `@TransactionalEventListener` |
| 一串有序校验 | 责任链 | Servlet `Filter` 链、Spring Security 过滤器链、Sentinel slot chain |
| 按类型扩展实现 | 策略 + 工厂 | Dubbo SPI、Spring `spring.factories`、ShardingSphere SPI |

## 1. IoC 让「工厂」和「单例」变成配置

`LogisticsRouter` 直接注入 `Map<String, LogisticsClient>`，key 就是 bean 名；新增承运商只要加一个 `@Bean("XXX")`，路由代码一行不改。`DiscountStrategyRouter` 演示的是等价的按 `type()` 建索引。

这比手写 `if (channel.equals("SF")) new SfAdapter()` 好在：实现可被替换、可单测、可利用生命周期回调。写 `static getInstance()` 则把这三样全丢了。

**注意**：Spring singleton 是容器级，不是 JVM 级；单例里不要放请求状态。

## 2. 顺序必须显式

注入 `List<X>` 时顺序由 `@Order` / `Ordered` 决定，不是 bean 名或扫描顺序。责任链、过滤器链、Sentinel slot chain 的顺序都是业务语义：统计必须在限流前、参数校验必须在风控前。靠字母排顺序迟早出事。

## 3. AOP 代理的三个陷阱

1. **自调用**：同类 `this.method()` 不走代理，`@Transactional`、`@Async`、`@Cacheable` 全失效。把方法拆到另一个 bean（本模块的外观与事务、切面分在不同类上就是这个原因）。
2. **接口 vs 类**：有接口默认 JDK 动态代理，没有接口用 CGLIB；`final` 类/方法不能被代理。
3. **代理不是原对象**：类型强转、`==` 比较、拿 `getClass()` 判断都会和预期不同。

`ServiceTimingAspect` 对 `PlaceOrderFacade` 的切面，`/api/dm/stats` 里能看到它记录的调用次数，说明代理确实生效。

## 4. 模板回调：模板方法 + 策略的合体

`JdbcTemplate.execute(sql, callback)` 里，连接获取、语句创建、异常翻译、资源释放是模板骨架，回调只是一段变化的策略。这比「模板方法靠子类继承」更松耦合：不同调用传不同 lambda，不用为每个 SQL 定义一个子类。

本模块的 `ReceiptTemplate` 用继承版讲清结构；生产里如果只是「一步不同」，优先用回调或函数式参数。

## 5. 事件时机比事件本身重要

| 注解 | 时机 | 适合 |
| --- | --- | --- |
| `@EventListener` | 同步、发布者同一事务 | 积分、审计这类要跟着回滚的 |
| `@TransactionalEventListener(AFTER_COMMIT)` | 事务提交后 | 短信、仓库通知、发 MQ |
| `@Async` + 事件 | 另一个线程 | 耗时、允许延迟、失败可补偿的 |

没有事务上下文时 `AFTER_COMMIT` 默认不触发；发布者抛异常回滚时，同步监听器也一起回滚，事务后监听器不会执行。这些不是「事件机制不稳定」，正是它该有的语义。

## 6. 作用域与条件装配

- `CartDraft` 用 `@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)`，`CartDraftProvider` 用 `ObjectProvider` 每次取，避免注入单例被永久复用。
- `KeyValueStore` 的两个实现用 `@ConditionalOnProperty(name = "dm.redis.enabled", havingValue = "false", matchIfMissing = true)` / `"true"` 切换，见 [07](07-mysql-redis.md)。这也是抽象工厂思路：一族实现由一个开关整体切换，不会混用。
- Web 场景还有 request / session 作用域，但要注意：把 request 作用域 bean 注入单例，需要 scoped proxy，否则启动就报错。

## 7. SPI / 扩展点是「工厂 + 策略」的框架化

Dubbo 的 `@SPI`、Sentinel 的 `SpiLoader` 加载 `ProcessorSlot`、ShardingSphere 的 SPI 分片算法，本质都是「按名字找实现 + 可插拔」。Spring Boot 的 `spring.factories` / `AutoConfiguration.imports` 是同一思路在启动阶段的应用。业务项目里不必自己造 SPI 框架，用 `Map<String, X>` 注入 + 配置化选择就够了。

## 结论

- 先查框架有没有：AOP、事件、作用域、条件装配、`*Template` 能解决大部分横切与扩展需求。
- 手写模式的合理场景是**业务语义本身**：状态转移表、优惠策略、规则树、快照、命令幂等、分页游标——框架不会替你决定业务规则。
- 引入一个模式的判据是「现在这段代码因为缺少它而难以修改」，不是「这个模式还没出现过」。
