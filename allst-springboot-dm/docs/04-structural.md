# 结构型：7 种「对象怎么组合」

结构型模式解决的是**在不把业务类撑爆的前提下，把不兼容的接口、多个子系统、横切能力组织起来**。这一类和 Spring 重叠最多：很多「模式」框架已经用代理和 IoC 替你实现了，重点是知道什么时候该借力、什么时候别再手写一遍。

代码在 `com.allst.springboot.structural`（外观在 `showcase`），测试是 `StructuralPatternsTest` 和 `PlaceOrderFlowTest`。

## 适配器 Adapter — `adapter/*`

**场景**：顺丰 SDK 是 `sfCreateOrder(address, orderId)` 返回字符串单号，京东 SDK 是 `createWaybill(orderId, address)` 返回自定义对象；业务层只想说「给这个订单发货」。

**不用它的写法**：Controller / Service 里直接引用 `SfExpressSdk`、`JdResult`，加一家渠道就全项目搜一遍改类型判断。

**落地**：统一 `LogisticsClient` 接口与 `ShipmentTicket` 回执，`LogisticsAdapters` 用两个 `@Bean("SF")` / `@Bean("JD")` 把第三方 SDK 包起来，`LogisticsRouter` 注入 `Map<String, LogisticsClient>` 按承运商编码路由。Spring 按 bean 名组装 Map 是这一模式在 Boot 项目里最实用的形态。

**解决的问题**：第三方类型不外泄；新增渠道只加一个 bean；返回语义统一（都是 carrier + 运单号 + 订单号）。

**同类**：Spring MVC 的 `HandlerAdapter` 让各种 handler 写法都能被 `DispatcherServlet` 调用；`spring-security` 的各类 provider 适配也是这个思路。

**什么时候不用**：接口是自己设计的、能直接改，就别多套一层；适配器是给「改不了的」用的。

## 桥接 Bridge — `bridge/NotificationBridge`

**场景**：消息种类（订单通知 / 告警）和发送通道（短信 / 邮件 / App 推送）两个维度都在变。

**不用它的写法**：`OrderSmsNotification`、`OrderEmailNotification`、`AlertSmsNotification`… 3×3=9 个类；加一个通道要改 3 处。

**落地**：`Notification` 是抽象层，只负责 `render()` 说什么；`Sender` 是实现层，只负责 `send()` 怎么送；抽象层持有 Sender 引用，`deliver()` 固定组合动作。两个维度各自扩展、运行时自由搭配。

**解决的问题**：类数量从 N×M 降到 N+M，新增通道或消息类型互不影响。

**Spring 里的写法**：真实项目里 `Sender` 通常是多个 `@Component`，由 `Map<String, Sender>` 选；这里收在一个类里是为了一眼看清桥的两端。JDBC 驱动与 SLF4J 绑定是同一种「抽象与实现分离」的结构。

**什么时候不用**：只有一个维度会变时，普通继承或策略就够了，桥接会多一层间接。

## 组合 Composite — `composite/PromotionRuleTree`

**场景**：优惠券规则是树——「满 100 且（VIP 或 新人）且非跨境单且风控通过」。

**不用它的写法**：为每层括号写一层 if，新增条件就重排整段逻辑，叶子条件和「一组条件」无法统一处理。

**落地**：`RuleNode` 接口同时由叶子（`LeafNode`）和组合节点（`AndNode` / `OrNode` / `NotNode`）实现，`evaluate(ctx)` 递归求值，`describe()` 能打印整棵树。调用方永远只调一次 `node.evaluate(ctx)`。

**解决的问题**：层级结构本身成为数据，规则可以配置化；加条件不改求值代码。

**边界**：规则一旦复杂到需要函数、时间窗口、优先级、互斥和解释执行，就该上 SpEL / Drools / Aviator（见 [05](05-behavioral.md) 的解释器）。组合模式擅长表达层级，不擅长表达语义。

## 装饰器 Decorator — `decorator/*`

**场景**：商品报价原本只查价目表，后来要加缓存，再后来要加耗时监控。

**不用它的写法**：每次都改 `BaseQuote`，最后它同时负责定价、缓存失效、埋点，任何一处改动都要回归另外两处。

**落地**：`PriceQuote` 接口 → `BaseQuote`（只定价）→ `CachedQuote`（缓存，TTL 60 秒）→ `TimedQuote`（计时）。`PriceQuoteService` 组装成 `Timed(Cached(Base))`，对外仍是 `PriceQuote`。`evict(sku)` 用于调价时失效缓存。

**解决的问题**：横切能力按需叠加、顺序可调；原实现和测试都不用改。缓存实现通过 `KeyValueStore` 注入，默认内存、可切 Redis，见 [07](07-mysql-redis.md)。

**同类**：MyBatis `CachingExecutor` 持有 `Executor delegate`，在委托前后加二级缓存；Spring `@Cacheable` 是代理式的同类能力。

**什么时候不用**：能力只有一项且不会增加时，直接写进原类更简单；装饰层数过多会让排查调用链变困难。

## 外观 Facade — `showcase/PlaceOrderFacade`

**场景**：一次下单要调用二十多个组件，还有事务边界和严格顺序（校验没过不能算价、状态没推进不能发事件）。

**不用它的写法**：Controller 里注入二十个服务、手写顺序和 try-catch，每个新同事都要重新理解一遍「为什么先发事件再落库会出事」。

**落地**：`place()` 一个入口编排全链路，`@Transactional` 定事务边界，`PlaceOrderResult.trace` 把经过的模式原样返回；`overview()` 提供子系统清单。

**解决的问题**：HTTP 层与子系统复杂度隔离；事务、顺序、幂等这些容易出错的细节只有一个地方写；AOP 计时切面只需挂在这一层。

**边界**：外观容易长成「上帝类」。当编排步骤开始有分支、状态、补偿时，就该拆成应用服务 + 领域服务，或引入状态机（见仓库的 `allst-springboot-state-machine`）。本模块故意保留一个较胖的外观，是为了把模式串起来给大家看。

## 享元 Flyweight — `flyweight/SkuSpecRegistry`

**场景**：一个订单几百行，每行都要显示品牌、类目、计税单位、税率、售后政策，这些和「买了几件、成交价多少」无关。

**落地**：`SkuSpec` 完全不可变，按 SKU 缓存在 `ConcurrentHashMap` 里共享；`describe(sku, quantity, dealPrice)` 把数量、价格作为外部状态由调用方传入，绝不写进享元对象。

**解决的问题**：避免重复创建/重复查询不变元数据；明确区分内部状态（共享）与外部状态（每次不同）。

**易混淆**：单例是全局一个实例；享元是每个 key 一个共享实例；缓存的重点是省一次查询且能失效，享元的重点是对象结构设计。享元对象必须不可变，否则一处改动会污染所有引用者。

## 代理 Proxy — `proxy/*`

**场景**：外部库存服务接口已定，调用方要记录调用前后、统一记失败原因；下单外观要计时。

**落地**：`JdkAuditProxy.wrap()` 用 JDK `Proxy.newProxyInstance` 生成代理，`InvocationHandler` 写 BEFORE / AFTER / FAILED 审计；`ServiceTimingAspect` 用 Spring AOP `@Around` 给 `PlaceOrderFacade` 计时。两份代码解决同一类问题，展示「手写代理」和「容器代理」的差别。

**解决的问题**：不改目标类即可控制访问、加横切逻辑；异常路径也能统一留痕。

**必须记住的坑**：
- **自调用**：同一个类里 `this.method()` 不经过代理，`@Transactional`、`@Async`、`@Cacheable` 都不生效。需要拆到另一个 bean，或用 `AopContext.currentProxy()`（不推荐）。
- JDK 动态代理要求目标有接口；没有接口时 Spring 用 CGLIB 生成子类，`final` 类和方法无法代理。
- 代理对象与原始对象不是同一实例，用 `==` 或类型强转前要想清楚。

**同类**：`@Transactional`、MyBatis Mapper 接口（`MapperProxy` 把方法调用翻译成 SQL）、Feign 客户端都是代理。

**验证**：`StructuralPatternsTest.proxy` 断言审计日志和失败记录；`DmApiTest` 断言 `/stats` 里能看到 AOP 代理计数。
