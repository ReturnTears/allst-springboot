# 参考的 GitHub 开源项目

本模块的业务场景（下单、促销、库存、支付、通知、回执、对账）参考了下面这些开源项目的**问题和结构**，但代码是重新写的，没有照搬任何仓库的源码或业务表。

模式归类是对实现结构的解读，不是项目官方说法。为避免把推断写成事实，分三档标注：

- **✅ 确认**：公开源码、官方文档或项目 README 可直接看到该结构。
- **△ 推断**：结构同型、或由公开架构资料合理推出，未逐行核对某个具体版本。
- **○ 通用场景**：没有特定源码对照，用常见业务形态说明。

## 项目与场景对照

| 项目 | 业务场景 | 对照的模式 | 程度 |
| --- | --- | --- | --- |
| [macrozheng/mall](https://github.com/macrozheng/mall) | 电商系统：商品/SKU、购物车、下单、促销、库存锁定、订单超时取消 | 外观（下单门户服务）、策略（促销）、适配器（第三方对接）、观察者（延迟消息/定时取消） | ✅ 项目结构确认；△ 具体类的模式归类 |
| [alibaba/Sentinel](https://github.com/alibaba/Sentinel) | 限流熔断：`SphU.entry` 后依次经过一组 Slot | 责任链（`ProcessorSlotChain`）、SPI 扩展策略 | ✅ |
| [apache/dubbo](https://github.com/apache/dubbo) | 按名字加载协议、序列化、线程池、路由等扩展实现 | 工厂方法/策略 + 适配器（`@SPI`、`ExtensionLoader`） | ✅ |
| [mybatis/mybatis-3](https://github.com/mybatis/mybatis-3) | Mapper 接口无实现却能执行 SQL；二级缓存 | 代理（`MapperProxy`）、装饰器（`CachingExecutor` 持有 `delegate`）、建造者（`SQL` / Example） | ✅ |
| [spring-projects/spring-framework](https://github.com/spring-projects/spring-framework) | 数据访问模板、事件、AOP、MVC 分派 | 模板方法/模板回调（`JdbcTemplate`）、观察者（`ApplicationEvent`）、代理（AOP）、适配器（`HandlerAdapter`）、单例（bean 作用域） | ✅ |
| [alibaba/COLA](https://github.com/alibaba/COLA) | `cola-component-statemachine` 无状态状态机、扩展点设计 | 状态、建造者（Builder 定义转移） | ✅ 模块存在；△ 分类 |
| [apache/shardingsphere](https://github.com/apache/shardingsphere) | 分片算法、读写分离等可插拔实现 | 策略 + SPI 工厂、抽象工厂（一族组件配套） | △ |
| [newbee-ltd/newbee-mall](https://github.com/newbee-ltd/newbee-mall) | 订单状态枚举与流转 | 状态 | △ |
| [xuxueli/xxl-job](https://github.com/xuxueli/xxl-job) | 任务处理器统一骨架 | 模板方法（`IJobHandler`）、命令 | △ |
| [apache/rocketmq](https://github.com/apache/rocketmq) / [apache/seata](https://github.com/apache/seata) | 消息消费、分布式事务回滚记录 | 命令、备忘录（undo log） | △ |
| [alibaba/nacos](https://github.com/alibaba/nacos) / [apolloconfig/apollo](https://github.com/apolloconfig/apollo) | 配置变更后客户端更新本地规则 | 单例规则缓存 + 观察者 | △ |
| [jeecgboot/JeecgBoot](https://github.com/jeecgboot/JeecgBoot) / [yangzongzhuan/RuoYi](https://github.com/yangzongzhuan/RuoYi) | 导入导出、数据权限树、字典 | 模板方法、组合、享元（字典） | △ |
| Servlet / Jakarta EE 规范 | 请求过滤链 | 责任链 | ✅ |

○ 的场景：享元（SKU 规格共享）、备忘录（客服改单快照）、中介者（支付成功后多方协作）、原型（活动模板复制）、访问者（多套报表口径）、迭代器（对账游标）。这些是通用业务形态，用来把模式讲清楚，不对应某个仓库的特定类。

## 几条明确核实过的论断

- **Sentinel 的 slot chain 是责任链**：`ProcessorSlotChain` 由 `SlotChainBuilder`（默认 `DefaultSlotChainBuilder`）通过 SPI 加载 `ProcessorSlot` 并按 `@Order` 排序串联；`StatisticSlot`、`FlowSlot`、`DegradeSlot` 等各管一件事，任一 slot 抛出 `BlockException` 就中断。顺序错了规则会失效——这正是本模块责任链一节强调「顺序是业务语义」的现实依据。
- **MyBatis `CachingExecutor` 是装饰器**：官方源码页可见 `private final Executor delegate`，`query()` 先查 `TransactionalCacheManager`，未命中才 `delegate.query(...)`。
- **MyBatis Mapper 是 JDK 动态代理**：`MapperProxyFactory.newInstance` 用 `Proxy.newProxyInstance` 生成接口实现，`MapperProxy.invoke` 把方法调用翻译成 SQL 执行。
- **Dubbo SPI 是可扩展的工厂/策略体系**：官方 SPI 列表文档列出了序列化、线程池、路由、配置中心等大量扩展点及激活条件。
- **mall 是完整电商系统**：项目 README/教程确认包含商品、订单、促销、权限、库存等模块，并包含下单锁库存、超时取消等场景。

## 只是合理推断的部分

「JDBC 驱动 / SLF4J 绑定 = 桥接」「Nacos/Apollo 规则缓存 = 单例」「业务规则树 = 组合」「ASM/ANTLR = 访问者」这些是按结构同型做的类比，相关项目并没有自称使用了某个 GoF 模式。同样，把 mall 的下单服务叫「外观」、把 xxl-job 的处理器叫「模板方法」，是对代码形状的解读，不是官方分类。文档里保留这些对照是为了给业务场景找落点，不作为源码事实引用。

## 想继续读源码

按这些关键词在对应仓库里搜，比按模式名搜有用：`ProcessorSlotChain` / `DefaultSlotChainBuilder`、`MapperProxyFactory` / `CachingExecutor`、`@SPI` / `ExtensionLoader`、`JdbcTemplate` / `HandlerAdapter` / `TransactionalApplicationListener`、`StateMachineBuilder`、`OmsPortalOrderService` / `CancelOrderReceiver`。

链接在 2026-11 核实可访问；开源项目会重构，具体类名和包路径请以当前版本为准。
