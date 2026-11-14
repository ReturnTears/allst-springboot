# 行为型：11 种「职责怎么分」

行为型模式在这一模块里最密集，因为真实业务的分歧大多不是「对象长什么样」，而是「谁来判断、谁来执行、谁被通知、顺序和时机由谁掌握」。

代码在 `com.allst.springboot.behavioral`，测试是 `BehavioralPatternsTest`、`PlaceOrderFlowTest`。

## 责任链 Chain of Responsibility — `chain/OrderValidationChain`

**场景**：下单前参数、风控、库存限购、优惠券、黑名单依次校验，任一环节可拒绝。

**不用它的写法**：一个方法里几十行 `if`，嵌套三层，还不知道是哪一环拦的；加一条规则要读完整段才知道插哪里。

**落地**：`Check` 接口只回答 `violation(request)`，`validate()` 顺序遍历、失败即短路，返回 `passedChecks` / `failedAt` / `reason`。顺序就是业务语义。

**解决的问题**：校验规则可增可删、可单测；线上拒绝请求能直接定位到环节。

**Spring 里的写法**：每个 Check 是 `@Component` + `@Order`，注入 `List<Check>` 后遍历。**不要依赖 bean 名字母序**——顺序是业务规则，必须显式指定。Sentinel 的 `ProcessorSlotChain` 同样是责任链，slot 通过 SPI 加载并按 `@Order` 排序，顺序错了会导致限流规则失效。

**什么时候不用**：只有两三个固定判断，写在一起更好读。

## 命令 Command — `command/*`

**场景**：支付回调、扣库存、发券这些动作来自 HTTP、定时任务、消息消费者，要统一处理重复提交、失败重试、撤销。

**落地**：`OrderCommand` 带 `id()`（同时是幂等键）、`maxAttempts()`、`execute()`、`undo()`；`CommandInvoker` 用 `KeyValueStore.putIfAbsent`（SETNX 语义）去重，失败重试，**彻底失败后释放幂等键**让上游能真正重跑，成功后压入撤销栈。

**解决的问题**：多入口不再各写一套补偿；幂等与重试策略集中；`id` 让同一次动作可追踪。

**易错点**：
- 失败后不释放幂等键，会导致后续重试全部被判为重复，动作永远不执行。
- 幂等键作用域要含业务身份（这里用 `SAVE-订单号`），不能只用方法名。
- 示例的撤销栈是单例，生产要按业务单据隔离，否则不同订单的撤销会串。

**同类**：MQ 消费者把消息当命令、Seata 的 undo log 是命令的反向记录。

## 解释器 Interpreter — `interpreter/PromotionExpression`

**场景**：运营要能配「满 100 且（VIP 或 新人）且风控通过」，改规则不能发版。

**落地**：给了两种。`demoAst()` 手写语法树，TerminalExpression（金额、VIP）与 NonTerminalExpression（AND/OR/NOT）组合求值，用来讲清模式结构；`evaluate()` 用 Spring 的 `SpelExpressionParser` 一行解析字符串表达式。

**解决的问题**：规则从代码变成数据；同一份上下文可以被不同表达式反复求值。

**明确建议**：生产里**不要自己写词法/语法/优先级解析器**，成本远高于引入 SpEL、Aviator 或 Drools。本模块的手写 AST 只用于教学，表达式越长越不划算。

**坑**：SpEL 默认能访问类型和方法，表达式若来自用户输入要配 `StandardEvaluationContext` 的类型白名单，避免任意方法调用。

## 迭代器 Iterator — `iterator/PagedOrderCursor`

**场景**：对账、导出、批量补发券要遍历几十万订单。

**落地**：`PagedOrderCursor implements Iterator<OrderRecord>`，`hasNext()` 在内存批次耗尽时用 `PageRequest.of(page, batchSize, Sort.by("id"))` 拉下一批，`next()` 只从当前批次取。调用方写 `while (cursor.hasNext())`。

**解决的问题**：内存里始终只有一批；分页样板不再在每个任务里复制。

**坑**：分页必须固定排序字段，否则翻页可能重复或漏行；遍历期间若有并发写入，边界也会变，需要快照或按 id 游标。

**同类**：MyBatis `Cursor`、Spring Batch `ItemReader`、JPA 流式结果。

## 中介者 Mediator — `mediator/OrderMediator`

**场景**：支付成功后库存转实扣、发积分、推通知、写审计，四个动作有顺序且都不该知道彼此存在。

**落地**：`Colleague` 只实现 `onPaid()`；`OrderMediator.paid()` 按固定顺序调用并返回每步结果。

**解决的问题**：把网状互相调用收成星形；顺序、跳过条件和以后要加的补偿逻辑集中在一处。

**和观察者怎么选**：中介者认识所有同事、主动决定顺序和是否中断；观察者里发布者不认识订阅者、不控制顺序。需要严格顺序用中介者，需要「随便挂多少个、彼此无关」用事件。两者常常一起用：中介者编排核心动作，事件广播非关键副作用。

## 备忘录 Memento — `memento/*`

**场景**：客服改单改错了要一键还原，风控要看改前是什么。

**落地**：`CartEditor` 自己 `save()` 出不可变 `Memento`（重新拷贝 `List<Line>`），`restore()` 也由它自己执行；`SnapshotCareTaker` 只 `push` / `undo` / `labels`，不看快照内容。

**解决的问题**：不暴露对象内部结构就能撤销；快照不会跟着对象一起变；负责人和原发器职责分离。

**坑**：浅拷贝的「快照」是最常见的假撤销；单例撤销栈会串单，生产要按单据/用户隔离，并考虑持久化与过期。工作流表单快照、undo log 是同一思路。

## 观察者 Observer — `observer/*`

**场景**：订单支付成功后发积分、发短信、写审计、通知仓库。

**落地**：`OrderPlacedEvent` 是普通 POJO 事件；`OrderEventListeners` 有三个 `@EventListener`（同事务同步执行）和一个 `@TransactionalEventListener(phase = AFTER_COMMIT)`（事务提交后才通知仓库）。发布者在 `PlaceOrderFacade` 里只 `publishEvent` 一次。

**解决的问题**：加一个后续动作不用改主流程；**事务回滚时外部消息还没发出去**，这是把通知写在业务方法里最容易出的事故。

**坑**：
- 没有事务上下文时 `AFTER_COMMIT` 监听器默认不触发，测试里不要指望「publish 了就一定执行」。
- `@EventListener` 默认同步、和发布者同事务，一个监听器抛异常会回滚整个下单。要隔离就加 `@Async`（记得配线程池和事务边界）或改成事务后事件。
- 事件链容易形成环（A 的事件触发 B，B 又发 A 的事件），要约束。
- 本模块没开异步，避免示例引入线程池这个额外变量。

**验证**：`PlaceOrderFlowTest.observersUseCorrectPhase` 断言事务内审计和提交后仓库通知都在正确阶段。

## 状态 State — `state/OrderStateMachine`

**场景**：订单 CREATED / PAID / SHIPPED / CLOSED / CANCELLED，支付回调、发货、取消、超时关闭可能并发到达。

**落地**：`Map<OrderStatus, Map<Event, OrderStatus>>` 转移表，`fire()` 查不到合法转移就抛 `BizException`，`allowed()` 列出当前状态可用事件。

**解决的问题**：非法事件到不了业务动作；新增状态改的是转移表，不用翻遍每个方法开头的 `if (status == ...)`。

**边界**：这是查表式简版，行为差异仍由调用方按新状态选择。需要守卫、动作、扩展状态、持久化恢复和并发控制时，用完整的 `allst-springboot-state-machine` 模块（Spring Statemachine / COLA）。

**坑**：状态转移必须和持久化、幂等在同一事务里，否则「状态已改、库没改」会造成重复发货。

## 策略 Strategy — `strategy/DiscountStrategyRouter`

**场景**：满减、会员折扣、新人券，促销规则每周变。

**落地**：`DiscountStrategy` 约定 `type()` 与 `discount(ctx)`；路由器按 type 索引，`bestType()` 选本次最大优惠，`apply()` 封顶不超过订单金额。阈值不写死在算法里，而是读单例 `RuleRegistry`。

**解决的问题**：算价主流程不再堆 if-else；单条规则可单独测试；上新活动只加一个实现。

**Spring 里的写法**：每个策略 `@Component`，服务注入 `Map<String, DiscountStrategy>` 或 `List<DiscountStrategy>` 后建索引。本模块把四个实现收在一个类里便于对照算法，索引和查找方式与注入版一致；`LogisticsRouter` 展示的是真实 Map 注入版。

**坑**：策略选择条件别再写成一个巨大的 `if`，那是把复杂度搬了个家；用注解标记 type、注册时自动收集。

## 模板方法 Template Method — `template/*`

**场景**：国内回执和跨境回执步骤相同（校验 → 取明细 → 算税 → 排版 → 落痕），只有明细和计税不同。

**落地**：`ReceiptTemplate.generate()` 用 `final` 固定骨架顺序，`loadLines()` / `calculateTax()` 抽象，`decorate()` 是可选钩子（跨境用它加报关清单），`record()` 私有收尾。`DomesticReceipt` / `CrossBorderReceipt` 只填差异。

**解决的问题**：改一处校验不会漏掉另一份拷贝；骨架顺序不会被某个子类悄悄改掉。

**同类**：`JdbcTemplate`（获取连接、异常翻译、释放资源由模板管，回调只写 SQL 映射）、`RestTemplate`、xxl-job 的 `IJobHandler`、批量导入基类。

**坑**：骨架方法不加 `final`，子类一覆盖又变成复制流程；钩子太多会让模板难懂，优先把变化收敛成两三个抽象方法。

## 访问者 Visitor — `visitor/OrderLineVisitorDemo`

**场景**：订单行有实物、虚拟商品、礼品卡，要按金额、发票品类、风控等多套口径分别计算。

**落地**：`OrderItem.accept(Visitor)` 双分派，由元素自己决定调哪个 `visitXxx`；`AmountVisitor` 算应收（礼品卡不计入营收），`InvoiceVisitor` 给发票品类。新增一套口径只加一个 Visitor。

**解决的问题**：不在每个实体里堆 `computeAmount()` / `invoiceCategory()`，也不在外层写满屏 `instanceof`。

**代价**：新增一种元素类型要改所有 Visitor 接口。所以它适合**元素类型稳定、操作经常增加**的场景（报表、导出、发票、风控口径）；如果商品类型本身每周变，用策略或注册表更合适。

**验证**：`BehavioralPatternsTest.visitor` 断言三行样例的应收金额是 3397（礼品卡 500 不计入）。
