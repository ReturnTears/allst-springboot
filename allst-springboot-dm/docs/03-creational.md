# 创建型：5 种「对象怎么来」

创建型模式解决的是**把「谁决定创建哪个对象、什么时候创建、怎么保证合法」从业务代码里挪出去**。在 Spring 项目里，容器已经接管了大部分 `new`，所以这一类的重点变成了：什么时候不该自己 new、什么时候该让容器按名字给你、什么时候该复制而不是重新装配。

代码在 `com.allst.springboot.creational`，测试是 `CreationalPatternsTest`。

## 单例 Singleton — `singleton/RuleRegistry`

**场景**：促销阈值、风控金额线、字典配置，一次大促里被每个下单请求读上百遍，但不该查上百次库。配置中心推送时还要能整体换一批。

**不用它的写法**：每个服务方法里 `configMapper.selectByKey(...)`；或者有人写了个 `static getInstance()`，把数据源、@Value 和 Mock 全部绕开，测试直接起不来。

**落地**：`@Component` 本身就是容器级单例；内部用 `ConcurrentHashMap` 存规则，`reload()` 批量替换，`snapshot()` 返回不可变副本。折扣策略和风控读的是同一个 `RuleRegistry`。

**解决的问题**：省掉重复查询；避免重载期间读到半新半旧的字典；避免手写单例破坏依赖注入。

**Spring 里的坑**：
- Spring 的 singleton 是「每个 IoC 容器一个实例」，不是 GoF 的「每个类加载器一个」。多个容器、多个 ClassLoader 下不保证全局唯一。
- 单例 bean 里放请求级可变状态（当前用户、当前订单）是事故源头。要放就用 `ThreadLocal` 或换成 prototype/方法参数。
- 真正的全局缓存、分布式配置一致性，交给 Redis / 配置中心，别指望进程内单例。

**什么时候不用**：只是无状态工具类，直接静态方法；需要按租户隔离的数据，别放单例。

**验证**：`CreationalPatternsTest.singleton` 断言容器里是同一实例、重载后计数 +1。

## 工厂方法 Factory Method — `factorymethod/PayChannelFactory`

**场景**：支付宝、微信、余额各有签名方式、流水号规则和限额判断。

**不用它的写法**：

```java
public PayReceipt pay(String channel, String orderNo, BigDecimal amount) {
    if ("ALIPAY".equals(channel)) {
        String serial = "ALI" + ...;
        // 签名、预下单、拼回执
    } else if ("WECHAT".equals(channel)) {
        ...
    }
    // 每接一个渠道改一次这里，还容易把公共流程复制错
}
```

**落地**：抽象 `PayCreator` 固定 `pay()` 骨架（创建命令 → 执行 → 拼回执），`createCommand()` 是工厂方法，由 `AlipayCreator` / `WechatCreator` / `BalanceCreator` 决定造哪个命令。这里同时能看到模板方法：两者常一起出现。

**解决的问题**：新增渠道只需加一个 Creator + 一个 Command，公共流程一行不改；渠道差异不会漏进主流程。

**Spring 里的写法**：更常见的是每个渠道做成 `@Component("ALIPAY")`，服务注入 `Map<String, PayHandler>`，按编码取（本模块的 `LogisticsRouter`、`DiscountStrategyRouter` 就是这个写法）。教科书结构保留在这里，是为了看清「创建」这一步被推迟到子类。

**什么时候不用**：只有一两种实现、且可预见不会增加时，一个 `switch` 反而更清楚。工厂的价值随实现数量增长。

**验证**：`factoryMethod` 覆盖三种渠道与未知渠道拒绝。

## 抽象工厂 Abstract Factory — `abstractfactory/ChannelKitFactory`

**场景**：国内站和跨境站各有自己的订单号号段、结算口径（跨境含综合税）、合规说明（跨境要报关清单）。

**不用它的写法**：单号、结算、合规三处各自 `if (跨境) ...`，某天新加一个渠道只改了其中两处，就出现「跨境单 + 国内发票」。

**落地**：`ChannelKit` 接口把三个产品捆成一族，`forChannel()` 一次给整套。调用方拿到 kit 后，`newOrderNo()` / `settle()` / `complianceNote()` 必然配套。

**解决的问题**：保证一族对象的一致性，把「组合是否合法」变成类型约束而不是开发者的记忆力。

**与工厂方法的区别**：工厂方法造一个产品，抽象工厂造一族产品；前者靠继承，后者通常靠组合 + 接口。

**什么时候不用**：各组件本来就相互独立、可以自由搭配时，抽象工厂只会增加一层接口。

**验证**：`abstractFactory` 断言跨境结算 100 → 109.00，国内 100 → 100.00。

## 建造者 Builder — `builder/PromotionRule`

**场景**：运营后台配置促销规则：编码、名称、类型、门槛、减免值、折扣系数、渠道白名单、互斥标签、优先级、生效区间、表达式……十来个字段，且有组合约束。

**不用它的写法**：`new PromotionRule(code, name, type, null, null, true, ...)` 没人看得懂；或者一路 setter，规则在必填项没齐时就被发布到缓存里。

**落地**：不可变对象 + 内部 `Builder`，`build()` 里集中校验（满减必须有减免金额、折扣系数在 `(0,1]`、生效区间合法），产出后集合是 `unmodifiableSet`。

**解决的问题**：把「对象合法」这件事从调用方约定变成构造时强制；不可变产物可以安全放进缓存、发给下游。

**Spring 里的坑**：Lombok `@Builder` 很方便，但不会替你做业务校验，也不会自动保证集合字段不可变；复杂对象仍需手写 `build()`。

**什么时候不用**：两三个字段、没有组合约束时，构造器或静态工厂更直接。

**验证**：`builder` 覆盖非法满减/折扣被拒、产物集合不可修改。

## 原型 Prototype — `prototype/*`

**场景**：每季大促都是「复制上一季模板，改活动名、加几个 SKU、换 banner」；另外每个请求要一份独立的购物车草稿。

**两个真实的坑**：
1. **浅拷贝串改**：`Object.clone()` 只复制 `List` / `Map` 的引用，一次活动 `addSku` 会把所有同模板活动一起改掉。`PromotionTemplate.copyAs()` 重新 `new ArrayList` / `new LinkedHashMap`。
2. **prototype 注入单例失效**：把 `@Scope("prototype")` 的 bean 直接 `@Autowired` 进单例服务，注入只发生一次，那个对象实际上被永久复用。`CartDraftProvider` 用 `ObjectProvider<CartDraft>.getObject()` 每次现取（也可 `@Lookup` 或 scoped proxy）。

**解决的问题**：免去重复装配、避免副本污染原型；把「一次请求一份」的生命周期表达成作用域而不是手工 new。

**Spring 里的坑**：原型 bean 的销毁回调不由容器调用；原型里持有连接、线程池这类资源时要自己关。Spring 的 prototype 是「每次注入/getBean 一个新实例」，和 GoF 的 `clone()` 不是一回事，本模块两种都演示了。

**什么时候不用**：对象便宜、字段少、创建成本远低于维护拷贝逻辑时，直接 new。

**验证**：`prototype` 断言副本加 SKU 不影响模板、两次取的草稿是不同实例。
