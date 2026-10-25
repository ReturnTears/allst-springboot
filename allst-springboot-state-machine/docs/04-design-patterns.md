# 用到的设计模式

状态机本身就是状态模式。本模块没有停在“用了状态模式”这一句上，因为一台能落库、能并发、能通知的机器还会把别的模式带进来。下面每个模式都对应具体的类，以及它换掉了哪一段原本会堆在服务里的代码。

## 状态（State）

`OrderState`、`ContractState`、`InvoiceState` 是状态。转移写在 `TradeOrderStateMachineConfig`、`ContractStateMachineConfig`、`InvoiceMachineFactory`。

收益：新增一种合法转移时，改的是配置，不是每个业务方法开头的状态判断。非法转移到不了动作，附属字段不会被写了一半。

Spring Statemachine 把状态对象、区域和伪状态都交给框架。COLA 更瘦，状态就是调用 `fireEvent` 时传入的那个枚举。两种都是状态模式，差别在机器里有没有上下文和层级。

## 策略（Strategy）

守卫和动作都是可以替换的策略。`HighRiskGuard` 只负责“算不算高风险”，阈值来自配置。换一条风控规则时替换这个守卫，转移的形状不变。

`RiskEvaluator` 和 `StockReader` 也是策略。生产实现可以去调远程风控和库存服务，测试用睡眠的模拟实现，或用单元测试里的假实现证明两次调用在时间上重叠。`ParallelPrecheckService` 依赖接口，不依赖模拟类。

收益：分流规则和流程骨架分开。策略可以单独测试。

## 责任链（Chain of Responsibility）

`GuardChain.all` 把 `AmountGuard` 和 `PayWindowGuard` 串成发起支付的一道门。前面的守卫失败后，后面的不再执行，拒绝原因留在扩展状态里。

收益：一条转移可以叠多个独立条件，配置处仍然只有一个 `guard(...)`。条件的顺序就是链的顺序。

## 观察者（Observer）

`TradeOrderEffectListener` 实现 `StateMachineListenerAdapter`，在 `TRANSITION_END` 记录“从哪到哪、事件是什么”。它不改状态，也不发通知。

收益：审计日志和业务动作解耦。以后要加指标，可以再挂一个监听器，不用改 `MarkPaidAction`。

通知没有放在监听器里。状态恢复时，监听器有机会看到一次“进入当前状态”，那会把仓库通知再发一遍。所以通知留在转移动作中，动作只在真正发生转移时执行。

## 拦截器（对机器调用的一层包装）

`OrderAuditInterceptor` 在 `preEvent` 里给消息补上 `machineAcceptedAt`。框架先走拦截器，再走守卫和动作。

收益：所有订单事件共享的横切步骤放在一处，例如受理时间、以后可以加的操作者校验。业务动作保持只关心自己的字段。

## 命令（Command）

`OrderEvent` 是命令类型。`OrderEventCommand` 带上操作者、事件号、备注、物流单号。模板把它们放进 `Message` 的 header。动作和守卫从这次消息里取，不从线程上下文里取。

收益：同一次转移的输入是明确的。幂等键就是 `eventId`。重放同一条命令不会产生第二次转移。

发票和合同用各自的 `*EventCommand`，形状相同。

## 备忘录（Memento）

`DefaultStateMachineContext` 是备忘录：当前状态，合同场景下还有两个子区域的状态。数据库存的是备忘录里的业务字段，不是整台机器的序列化对象。

`OrderMachineTemplate` 和 `ContractMachineTemplate` 在每次事件前用备忘录恢复，事件后把新状态写回订单或合同。机器实例随后停止。

收益：进程重启后，下一笔事件仍从数据库里的状态继续。不需要把 Spring 的机器对象放进 Redis 才能活过一次请求。

## 工厂（Factory）

`@EnableStateMachineFactory` 产生 `tradeOrderMachineFactory` 和 `contractMachineFactory`。`getStateMachine(业务号)` 每次给一台新机器，并把业务号设成机器 id。

`InvoiceMachineFactory.getOrBuild` 构建 COLA 机器。COLA 的 `build` 会按 id 注册进静态工厂，重复构建会抛错，所以这里做成“已有就取出”。

收益：定义是一份，运行实例按业务单据分开。一台机器不会同时代表两笔订单。

## 建造者（Builder）

Spring 的配置适配器是一份建造过程：`withStates`、`withExternal`、`withChoice`、`withFork`、`withJoin`。COLA 的 `StateMachineBuilderFactory.create()` 用 `from / to / on / when / perform` 把同一份定义写完，再 `build`。

收益：转移表集中在一个类型里，读配置就能画出状态图，不用在服务之间来回跳。

## 模板方法（Template Method）

`OrderMachineTemplate.fire` 和 `ContractMachineTemplate.fire` 固定了“停止、恢复、启动、发送、再停止”。子步骤里变化的是备忘录怎么拼、扩展状态放什么。

收益：恢复顺序错了会导致子状态回到初始值，或把上一笔请求的扩展状态漏给下一笔。顺序写死在模板里之后，业务服务只提供当前状态和这条命令。

## 外观（Facade）

`TradeOrderAppService`、`ContractAppService`、`InvoiceAppService` 是给控制器的外观。控制器不知道工厂、备忘录和事务边界。外观负责：写操作加锁，再把调用交给事务服务；查询直接读库。

收益：机器生命周期这种基础设施细节不会漏进 Controller，也不会在每个事件方法里复制一遍。

## 仓储（Repository）

`TradeOrderRepository`、`SkuStockRepository`、`SalesContractRepository`、`InvoiceApplyRepository`、`TransitionLogRepository` 是仓储。库存扣减用仓储上的条件更新，而不是先查实体再 `setAvailable`。

收益：持久化和领域服务分开。条件更新把“库存是否足够”收成一条语句，调用方不用自己拼 SQL 字符串。

## 单例定义，原型实例

配置类、守卫、动作、COLA 机器都是 Spring 单例。Spring Statemachine 的运行实例是工厂按次创建的，用完即停。

COLA 的单例可以多线程同时 `fireEvent`，因为状态在参数里，不在机器里。Spring 的实例不行，同一台实例不能交给两个线程。这是选型差异，不是注解上的偶然。

收益：重量级定义只加载一次。请求之间不共享可变的当前状态。

## 代理（Proxy）

`@Transactional` 加在 `*FlowService` 上，由 Spring AOP 代理开启事务。`TradeOrderAppService` 先加锁，再调用另一个 bean 的 `fire`，这样事务从代理进入，锁包住整个事务。

如果在同一个类里自己调用 `@Transactional` 方法，代理不会生效，锁和事务的先后也会错。所以加锁和事务分在外观和流程服务两个类上。

收益：库存更新、状态回写、流水插入要么一起成功，要么一起回滚。业务代码里没有 `commit` 和 `rollback`。

## 这些模式合在一次支付成功里

```text
外观 TradeOrderAppService 加锁
  代理打开事务
    模板恢复备忘录并发送命令 PAY_SUCCESS
      拦截器补受理时间
      动作 MarkPaidAction 写支付时间
      策略 HighRiskGuard 选择下一状态
      观察者记下这次转移
    仓储写入状态和流水
  事务提交
线程池执行提交后通知
```

每一层都可以单独替换：换守卫不影响外观，换通知方式不影响转移表。
