# 目标与读法：为什么是一条下单主线

## 模块要回答什么

常见的设计模式资料停在「一个类图 + 一个 Animal/Duck 例子」。真实业务里没人会问「怎么给鸭子加飞行行为」，问的是：

- 下单校验有七八道，每道都可能拒绝，还要求知道是哪道拦的 → 用什么？
- 促销规则每周变，算价方法里的 `if (vip) ... else if (coupon) ...` 已经三百行 → 怎么拆？
- 支付成功后要发积分、发短信、通知仓库、写审计，但事务回滚了短信不能发出去 → 怎么排？
- 对接了三家物流 SDK，方法名和返回值都不一样，源码改不了 → 怎么统一？
- 客服改单要能撤销，改了什么还要留痕 → 快照放哪？

所以本模块按「**场景 → 不用模式的痛点 → 模式 → 解决了什么问题 → 对应类 → 什么时候别用**」六段式来写，每种模式都落到一段能跑的代码上，而不是停在概念。

## 为什么用一条主线

模式很少单独出现。一笔订单会同时需要创建、结构、行为三类模式，而且它们之间有先后关系：校验没过就不该算价，状态没推进就不该发事件，消息不该在事务提交前发出去。做成 23 个互不相关的 demo，读者学完仍然不知道怎么在一个真实服务里排布。

主线在 `showcase.PlaceOrderFacade.place()`，一次调用里依次经过：

```text
HTTP 请求
  └─ 外观 PlaceOrderFacade（AOP 计时代理在外层，事务从这里开始）
       ├─ 责任链 OrderValidationChain          参数→风控→限购→优惠券→黑名单，失败短路
       ├─ 组合   PromotionRuleTree             规则树求值
       ├─ 解释器 PromotionExpression          SpEL 表达式求值
       ├─ 策略   DiscountStrategyRouter        选最优优惠，阈值来自单例规则缓存
       ├─ 建造者 PromotionRule                把生效规则固化成不可变对象
       ├─ 抽象工厂 ChannelKitFactory          国内/跨境一整套：单号、结算、合规
       ├─ 享元   SkuSpecRegistry              共享 SKU 不变元数据
       ├─ 装饰器 PriceQuoteService           Timed(Cached(Base)) 报价
       ├─ 原型   PromotionTemplateRegistry   活动模板深拷贝 / prototype 草稿
       ├─ 工厂方法 PayChannelFactory          按渠道创建支付命令
       ├─ 状态   OrderStateMachine            CREATED --PAY--> PAID
       ├─ 命令   CommandInvoker               幂等 + 重试 + 撤销栈，落库
       ├─ 中介者 OrderMediator                协调库存/积分/通知/审计
       ├─ 观察者 OrderPlacedEvent             事务内同步监听 + AFTER_COMMIT 监听
       ├─ 备忘录 CartSnapshot                 改单前快照入栈
       ├─ 模板方法 ReceiptService             按渠道生成回执
       ├─ 访问者 OrderLineVisitorDemo         换口径重算订单行
       ├─ 适配器 LogisticsRouter              屏蔽顺丰/京东 SDK 差异
       ├─ 桥接   NotificationBridge          消息种类 × 发送通道
       └─ 代理   JdkAuditProxy               调用前后留痕
  事务提交 → 触发提交后的仓库通知
```

`PlaceOrderResult.trace` 会把这段顺序原样返回，`GET /api/dm/demo/{pattern}` 则可以把其中任意一个模式单独拎出来跑。

## 模式分类在这条链路里的位置

| 类别 | 关心什么 | 在主线里的角色 |
| --- | --- | --- |
| 创建型（5） | 对象怎么产生 | 规则、活动、支付命令、渠道组件、草稿，各有不同的产生理由 |
| 结构型（7） | 对象怎么组合 | 把第三方 SDK、缓存层、多个子系统包装成调用方看得懂的形状 |
| 行为型（11） | 职责怎么分配 | 决定谁判断、谁执行、谁被通知、顺序和时机由谁掌握 |

这张分类表只用于索引。真实选型时不用先问「这是结构型还是行为型」，直接查 [02-scenario-map.md](02-scenario-map.md)。

## 怎么验证

- `mvn -pl allst-springboot-dm test`：33 个测试，覆盖 23 种模式的关键行为、端到端下单、幂等、异常和 HTTP 接口，全部用 H2 内存库，不需要外部服务。
- `GET /api/dm/patterns`：机器可读的场景速查表，与 [02](02-scenario-map.md) 同源。
- `GET /api/dm/stats`：有状态的模式（单例规则、享元池、装饰链命中、AOP 代理计数、事件轨迹、命令流水、备忘录栈）留下的运行痕迹。

## 与仓库其它模块的关系

`allst-springboot-state-machine` 讲的是完整状态机（Spring Statemachine / COLA，含守卫、动作、持久化和并发）；本模块的 `behavioral.state.OrderStateMachine` 只保留一张转移表，用来说明状态模式本身。两者不重复，复杂流程请看前者。

`allst-springboot-norm`、`allst-springboot-redis` 分别讲工程规范和 Redis 用法；本模块的 Redis 只是可选实现，用来说明装饰器与命令幂等怎么切换存储，见 [07-mysql-redis.md](07-mysql-redis.md)。
