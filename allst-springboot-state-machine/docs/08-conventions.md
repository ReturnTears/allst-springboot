# 使用约定

这些约定来自这个示例真正跑通过的路径。违反其中几条时，测试会先失败：子状态回退、通知重放，或者守卫拒绝被说成“状态没变”。

## 什么时候引入状态机

引入的信号是：状态数量上来之后，每个写操作开头都在重复“当前状态是否允许”，而且分支还会再长。订单履约和合同双轨属于这一类。

不要为了两个状态、三个事件去建 Spring Statemachine。那种链用 COLA 或一张转移表就够，发票就是这样处理的。

## 写新转移时要同时动的地方

1. `*StateMachineConfig` 或 `InvoiceMachineFactory` 里的边。
2. 对应的 `*EventCatalog`，否则页面按钮和机器不一致。
3. 需要新数据时，加守卫或动作，不要把 if 写回 `*FlowService`。
4. 补一条集成测试，至少覆盖新边上的成功和一次非法事件。

`*FlowService` 只保留：幂等、调用模板、回写状态、记流水。

## 状态字段只有一个写入者

动作可以写支付时间、物流单号、驳回原因。`status` / `phase` 由流程服务根据机器结果写入。

库存不走“查出来再 set”。占库存用 `SkuStockRepository.deductIfEnough`。守卫上的 `stockReserved` 用来挡住同一次求值扣两次。

## 实例的用法

- 从工厂按业务号取实例。
- 先按数据库状态恢复，再 `start`，再 `sendEvent`，最后在 `finally` 里 `stop`。
- 不把实例放进单例字段，不交给线程池。
- 售后子状态恢复时带上父状态 `AFTER_SALE` 和子上下文。合同并行审核恢复时带上两个区域。
- 以状态是否变成另一个稳定业务状态来判断成功。不要只看 `sendEvent` 的布尔值。

## 幂等

调用方每次操作带上 `eventId`。同一 `machine_type + machine_id + eventId` 第二次到来时返回当前视图，`replayed = true`，不重复扣库存，也不多写流水。

超时扫描的事件号固定为 `timeout-{orderNo}`，同一张订单只会被系统关闭一次。

## 事务和通知

会回滚的工作留在事务里：状态、库存、流水、支付时间。

会调用外部系统的工作登记到 `AfterCommitExecutor`。事务没提交，通知不会发出。

监听器只做观察。恢复状态时不要在监听器里发通知。

## 时间

业务时间走 `AppClock`。支付时限和超时扫描用同一个时钟。测试用 `freeze` / `unfreeze`，生产代码不要调用 `freeze`。

## 安全和演示数据

操作者、发票角色来自请求体，用来把命令跑通。这不是权限模型。接入登录之后，操作者和角色应从认证信息写入命令，请求体里的同名字段应被忽略。

`DemoStockInitializer` 只在非 test 环境补 `SKU-PHONE` 和 `SKU-GIFT`。测试自己准备库存，避免和演示数据耦合。

## 数据库

默认连接本机 MySQL：库 `allst_state_machine`，用户 `root`，密码为空，`createDatabaseIfNotExist=true`，`ddl-auto: update`。

测试配置 `application-test.yml` 使用内存库，不要求安装 MySQL。不要把测试配置当成生产配置启动。
