# 场景速查表：什么场景用哪个模式，解决什么问题

这是本模块最常用的一页。`GET /api/dm/patterns` 返回同样的内容（`showcase.PatternCatalog`），改文档时记得同步改代码里的条目。

「解决的问题」一栏写的是**去掉哪一段代码 / 避免哪一类事故**，不是模式定义。

## 创建型

| 需求场景 | 用哪个模式 | 主要解决什么问题 | 对应代码 | 开源参考 |
| --- | --- | --- | --- | --- |
| 促销/风控阈值被每个请求反复读，但不想每次查库 | 单例 Singleton | 省掉重复查询；用容器单例 + 并发容器，避免手写 `getInstance()` 绕过 IoC，也避免读到重载了一半的字典 | `creational.singleton.RuleRegistry` | Nacos / Sentinel 规则缓存 |
| 支付宝/微信/余额各有签名、流水号、限额 | 工厂方法 Factory Method | 把「创建哪种支付命令」下放给 Creator，主流程不再到处 `switch` 渠道 | `creational.factorymethod.PayChannelFactory` | Dubbo SPI、mall |
| 国内站与跨境站要用各自的一整套：单号、结算、合规说明 | 抽象工厂 Abstract Factory | 一次给一族组件，杜绝「跨境单配国内结算」的非法组合 | `creational.abstractfactory.ChannelKitFactory` | ShardingSphere 多实现族 |
| 运营配置促销规则，十来个字段且互相有约束 | 建造者 Builder | 消灭不可读的全参构造器和字段没填全就被发布的半成品对象，校验收在 `build()` | `creational.builder.PromotionRule` | MyBatis `SQL` / Example Builder |
| 每季活动都是从模板复制后改名、改 SKU 白名单 | 原型 Prototype | 免去重新装配；**深拷贝**避免副本串改原型；prototype 作用域用 `ObjectProvider` 现取，避免注入单例后被永久复用 | `creational.prototype.*` | 通用业务场景 |

## 结构型

| 需求场景 | 用哪个模式 | 主要解决什么问题 | 对应代码 | 开源参考 |
| --- | --- | --- | --- | --- |
| 对接顺丰/京东等 SDK，方法名、参数顺序、返回类型都不同且源码不可改 | 适配器 Adapter | 统一成 `LogisticsClient`，第三方类型不外泄，新增承运商只加一个 bean | `structural.adapter.*` | Spring MVC `HandlerAdapter`、mall 第三方对接 |
| 订单/告警消息 × 短信/邮件/App 推送 | 桥接 Bridge | 避免 N×M 个继承子类；两个维度各自扩展、运行时组合 | `structural.bridge.NotificationBridge` | JDBC 驱动 / SLF4J 绑定（结构同型） |
| 优惠券规则是「满 100 且（VIP 或 新人）且非跨境」这样的树 | 组合 Composite | 叶子和组合实现同一接口，调用方只 `evaluate` 一次，不用为每层括号改 if 嵌套 | `structural.composite.PromotionRuleTree` | 规则树 / 类目树 |
| 报价服务后来要加缓存、再加耗时监控，但不想改原类 | 装饰器 Decorator | 横切能力按层套上去，顺序可调；原实现只管定价 | `structural.decorator.*` | MyBatis `CachingExecutor`、Spring `@Cacheable` |
| 一次下单要编排二十多个服务和事务边界 | 外观 Facade | Controller 不再堆服务调用；事务边界和顺序收在一处 | `showcase.PlaceOrderFacade` | mall 下单服务 |
| 几百个订单行都要显示品牌、类目、税率等不变元数据 | 享元 Flyweight | 每 SKU 共享一份不可变内部状态，省内存也省查询；外部状态（数量、成交价）由调用方传入 | `structural.flyweight.SkuSpecRegistry` | 字典/规格共享，通用场景 |
| 不能改目标类，又要加审计/事务/耗时/远程调用 | 代理 Proxy | 用 JDK/CGLIB 代理拦截，横切逻辑集中；同时要避开同类自调用导致注解失效 | `structural.proxy.*` | Spring `@Transactional`、MyBatis Mapper 代理 |

## 行为型

| 需求场景 | 用哪个模式 | 主要解决什么问题 | 对应代码 | 开源参考 |
| --- | --- | --- | --- | --- |
| 下单前参数、风控、限购、优惠券、黑名单依次校验 | 责任链 Chain of Responsibility | 几十行嵌套 if 变成一串只回答放行/拒绝的节点，失败短路并留下是哪一环 | `behavioral.chain.OrderValidationChain` | Sentinel `ProcessorSlotChain`、Servlet Filter |
| 支付回调/扣库存/发券要支持去重、重试、撤销 | 命令 Command | 动作带 id 和 undo，统一幂等键、重试次数、撤销栈；多入口不再各写一套补偿 | `behavioral.command.*` | MQ 消费者、Seata undo log |
| 运营要配置「满 100 且 VIP」这类门槛，改规则不能发版 | 解释器 Interpreter | 语法树可组合求值；生产直接用 SpEL/规则引擎，不自己维护解析器 | `behavioral.interpreter.PromotionExpression` | Spring SpEL、Drools、COLA |
| 对账/导出要遍历几十万笔订单 | 迭代器 Iterator | 不一次 `findAll()` 打爆内存，也不在每个任务里重写分页样板；固定排序避免漏行 | `behavioral.iterator.PagedOrderCursor` | MyBatis `Cursor`、Spring Batch `ItemReader` |
| 支付成功后库存、积分、通知、审计要按顺序协作 | 中介者 Mediator | 几个服务不再互相直接调用织成网，顺序与容错集中编排 | `behavioral.mediator.OrderMediator` | 通用业务场景 |
| 客服改单要能撤销，还要留「改了什么」 | 备忘录 Memento | 不可变快照由原发器自己产生/消费，负责人只保管；避免暴露内部字段或快照跟着对象变 | `behavioral.memento.*` | 工作流表单快照、undo log |
| 支付成功后发积分、短信、审计、仓库通知 | 观察者 Observer | 加后续动作不改主流程；`AFTER_COMMIT` 保证事务回滚时消息还没发出 | `behavioral.observer.*` | Spring `ApplicationEvent`、mall 订单超时取消 |
| 订单有已创建/已支付/已发货/关闭/取消，事件并发到达 | 状态 State | 转移表集中定义，非法事件到不了业务动作；新增状态不用翻遍每个方法开头的状态判断 | `behavioral.state.OrderStateMachine` | COLA 状态机、newbee-mall 订单状态 |
| 满减、会员价、新人券多种算价，每周上新 | 策略 Strategy | 算法各自成类按 type 路由，算价主流程不再堆 if-else，单条规则可单独测 | `behavioral.strategy.DiscountStrategyRouter` | ShardingSphere 分片策略、mall 促销 |
| 国内/跨境回执步骤相同，只有明细和计税不同 | 模板方法 Template Method | `final` 骨架固定步骤顺序，子类只填差异和钩子；改一处校验不会漏掉另一份拷贝 | `behavioral.template.*` | Spring `JdbcTemplate`、xxl-job `IJobHandler` |
| 实物/虚拟/礼品卡行要按金额、发票、风控多套口径算 | 访问者 Visitor | 新增一套计算只加一个 Visitor，不在每个实体堆方法、也不写满屏 `instanceof` | `behavioral.visitor.OrderLineVisitorDemo` | ASM/ANTLR 同类结构，业务侧报表口径 |

## Spring 已经替你做掉的部分

| 你想做的事 | 框架机制（别手写） | 说明见 |
| --- | --- | --- |
| 按类型替换实现、把扩展点变成配置 | IoC / DI，注入 `Map<String, X>` 或 `List<X>` | [06](06-spring-patterns.md) |
| 加事务、日志、限流、耗时 | AOP 代理 | [06](06-spring-patterns.md) |
| 资源获取/释放/异常处理的重复样板 | `JdbcTemplate` 等模板回调 | [06](06-spring-patterns.md) |

## 按症状反查

| 代码里出现这个症状 | 优先考虑 |
| --- | --- |
| 一个方法里 `if/else if` 按类型、渠道、会员等级越堆越长 | 策略；若是「当前状态决定能不能做」则是状态 |
| 每加一个「成功后要做的事」就改一次主方法 | 观察者 |
| 多个服务互相 `@Autowired`，形成环 | 中介者或事件解耦 |
| 第三方 SDK 的类型泄漏到了业务层 | 适配器 |
| 想加缓存/日志/重试但不能改原类 | 装饰器，或直接用 AOP |
| 两个子类维度各自扩展，组合出大量类 | 桥接 |
| 几个组件必须配套使用，分开选会拼错 | 抽象工厂 |
| 构造器十几个参数，或 setter 造出半成品 | 建造者 |
| 同一个不变对象被创建千万次 | 享元；若全局唯一则是单例 |
| 改了副本却把模板/原型改坏了 | 原型深拷贝 |
| 校验顺序有业务含义，失败原因找不到 | 责任链 |
| 状态判断散落在每个方法开头 | 状态 |
| 批量处理把内存打满 | 迭代器 / 游标 |
| 需要撤销、回滚、留改前快照 | 备忘录 + 命令 undo |
| 规则要由运营配置，改一次发一次版 | 解释器（优先 SpEL / 规则引擎） |
| 元素类型稳定，但计算口径不断增加 | 访问者 |
| Controller 里堆了二十个服务调用 | 外观 |

## 容易选错的一组

- **策略 vs 状态**：都长得像「一堆实现」，但策略由调用方选（这次用哪种优惠），状态由当前状态决定允许什么（已取消的订单不能发货）。
- **观察者 vs 中介者**：观察者里发布者不认识订阅者、不控制顺序；中介者认识每个同事并主动编排顺序。
- **装饰器 vs 代理**：装饰器强调「叠加能力且对外仍是同一接口」，代理强调「控制访问」；Spring AOP 兼具两者意味，别纠缠名词。
- **适配器 vs 外观**：适配器转换一个不兼容的接口，外观简化一个复杂子系统；前者被迫，后者主动。
- **享元 vs 单例 vs 缓存**：单例全局一个实例；享元按 key 一族共享不可变对象；缓存是省一次查询、可随时失效。
