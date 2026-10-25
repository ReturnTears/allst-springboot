# 一步步把履约流程收进状态机

下面每一步都对应仓库里的类。顺序是故意排过的：先把“能不能走”说清楚，再把“走了之后做什么”拿走，最后才处理持久化和线程。

## 第 1 步：把状态和事件收成枚举

`OrderState` 是业务能停住的位置，例如待支付、待发货、退货已申请。`OrderEvent` 是一次意图，例如发起支付、发货、同意退货。

`PAY_ROUTE`、`ALLOCATE_ROUTE`、`AFTER_SALE` 标记了 `structural`。它们是机器内部的分流或父状态，业务表里不会停在这些值上。`OrderStates.leaf` 在写库前会丢掉这些结构性状态，只留业务状态。

这一步之后，非法组合不再是一串字符串比较。调用方必须交出一个 `OrderEvent`。

## 第 2 步：用目录告诉页面现在能点什么

`OrderEventCatalog` 按当前状态列出允许的事件。`TradeOrderAssembler` 把这份列表放进 `allowedEvents`。

前端渲染按钮时读这个字段，不再自己写 `if (status == WAIT_SHIP)`。目录和 `TradeOrderStateMachineConfig` 必须一起改。集成测试走完主路径后，完成态的 `allowedEvents` 为空，用来卡住“终态还能点按钮”这种回归。

## 第 3 步：把“能不能走”抽成守卫

守卫实现 `Guard<OrderState, OrderEvent>`，返回 true 才允许这条转移。

| 守卫 | 拒绝或分流的条件 |
| --- | --- |
| `AmountGuard` | 金额不是正数 |
| `PayWindowGuard` | 超过 `statemachine.trade.pay-timeout-minutes` |
| `GuardChain.all` | 把上面两个串起来，前面失败就不再看后面 |
| `HighRiskGuard` | 风险分达到阈值时走人工审核，没达到走待占库存 |
| `ReserveStockGuard` | 条件更新库存，成功才进入待发货 |
| `ShipmentNoGuard` | 发货报文里没有物流单号 |

`GuardChain.reject` 把原因放进扩展状态。`TradeOrderFlowService` 在状态没有变化时优先把这个原因抛出去，所以调用方看到的是“已超过支付时限”或“发货需要物流单号”，而不是笼统的状态错误。

流程方法里不再出现 `if (score >= 80)` 或 `if (shipmentNo == null)`。

## 第 4 步：把“走了之后做什么”抽成动作

动作实现 `Action<OrderState, OrderEvent>`，只在转移被接受后执行。

| 动作 | 做的事 |
| --- | --- |
| `MarkPaidAction` | 写 `paidAt` |
| `ShipAction` | 写物流单号和 `shippedAt` |
| `NotifyWarehouseAction` | 登记“事务提交后再通知仓库” |
| `NotifyRefundAction` | 登记退款通知 |
| `NotifyCompletedAction` | 登记完成通知 |

状态列 `status` 不在动作里改。动作如果也 `setStatus`，就会和流程服务抢同一个字段。约定是：动作改附属字段，`TradeOrderFlowService` 用机器给出的叶子状态覆盖 `status`。

通知类动作不直接打远程接口。它们调用 `AfterCommitExecutor`，等本笔事务提交成功再进线程池。事务回滚时 `afterCommit` 不会跑，仓库不会收到一张已经作废的拣货通知。

## 第 5 步：用 Choice 替换支付和库存上的 if/else

配置里的两段：

```text
PAYING --PAY_SUCCESS / MarkPaidAction--> PAY_ROUTE
PAY_ROUTE --风险分达阈值--> RISK_REVIEW
PAY_ROUTE --否则--> WAIT_ALLOCATE

WAIT_ALLOCATE 或 STOCK_SHORT --ALLOCATE--> ALLOCATE_ROUTE
ALLOCATE_ROUTE --占库存成功 / 通知仓库--> WAIT_SHIP
ALLOCATE_ROUTE --否则--> STOCK_SHORT
```

`PAY_ROUTE` 和 `ALLOCATE_ROUTE` 是选择伪状态，机器不会停在上面。一次 `sendEvent` 结束时，叶子状态已经是 `RISK_REVIEW`、`WAIT_ALLOCATE`、`WAIT_SHIP` 或 `STOCK_SHORT`。流水只记业务上的前后状态，例如 `PAYING -> WAIT_ALLOCATE`，不记中间的分流点。

库存这支选择有一个必须说清的取舍。`ReserveStockGuard` 不只是查询，它执行：

```text
update sm_sku_stock
set available = available - :qty
where sku_code = :sku and available >= :qty
```

更新行数为 1 才返回 true。判断和占库存是同一条语句，两笔订单抢最后一件时只有一笔进入待发货。守卫里做写操作通常是坏味道，这里是为了让“够不够”和“占没占住”无法被拆成两次查询。`stockReserved` 标记防止同一次事件里守卫被重复求值时扣两次。这个更新参加外层事务，后面的步骤失败会把库存加回去。

## 第 6 步：用层级状态把售后从主流程里收进去

签收之后如果再平铺“退货中、同意、拒绝”，主流程的每个方法都要记得售后标志。配置把售后收成父状态 `AFTER_SALE`，下面只有三个子状态：

```text
DELIVERED --APPLY_RETURN--> AFTER_SALE / RETURN_APPLIED
RETURN_APPLIED --APPROVE_RETURN--> RETURN_APPROVED --START_REFUND--> REFUNDING
RETURN_APPLIED --REJECT_RETURN--> RETURN_REJECTED --CLOSE_AFTER_SALE--> DELIVERED
```

拒绝退货后先回到已签收，才能确认完成。页面在 `RETURN_REJECTED` 上只能看到“关闭售后”，看不到“确认完成”。这是目录和层级一起约束出来的，不需要在确认方法里再写 `if (afterSaleOpen)`。

落库只存叶子，例如 `RETURN_APPROVED`。下次恢复时不能把这个叶子直接塞进根机器，否则子机器会回到自己的初始状态 `RETURN_APPLIED`。`OrderMachineTemplate.contextOf` 写成父上下文 `AFTER_SALE` 加一个子上下文。这个恢复方式和集成测试 `returnRejectedThenComplete`、`returnApprovedThenRefund` 绑在一起。

## 第 7 步：用模板方法固定机器的生命周期

`OrderMachineTemplate.fire` 的顺序固定：

1. `StateMachineFactory.getStateMachine(orderNo)` 拿一台新实例，机器 id 就是订单号。
2. `stop`，避免自动启动停在初始状态。
3. 装上 `OrderAuditInterceptor`，给这条消息补受理时间。
4. `resetStateMachine`，用数据库里的状态作为备忘录。
5. `start`。
6. 把金额、风险分、SKU、数量、下单时间放进扩展状态，把操作者和物流单号放进消息头。
7. `sendEvent`。
8. 读叶子状态和拒绝原因。
9. `finally` 里 `stop`。实例不放回池子，也不交给别的线程。

业务服务不再自己拼这些步骤。合同有一份同样结构的 `ContractMachineTemplate`。

这里有一个框架行为要记住：`sendEvent` 返回 true，只表示事件进了机器的队列，不表示转移成功。`TradeOrderFlowService` 以“叶子状态变了，并且不是结构性状态”作为成功。没变就看拒绝原因。

## 第 8 步：状态变更、流水、附属字段放在同一个事务

`TradeOrderAppService.fire` 先按 `trade:{orderNo}` 加锁，锁住之后才调用带 `@Transactional` 的 `TradeOrderFlowService.fire`。锁覆盖整个事务，不会在提交前被下一笔请求读到旧状态。

事务里的顺序：

1. 用 `(TRADE_ORDER, orderNo, eventId)` 查流水。已有则直接返回，`replayed = true`，机器不会再跑。
2. 恢复并发送事件。动作在这次调用里改 `paidAt`、物流单号或库存。
3. 用叶子状态回写 `status`。
4. 插入 `sm_transition_log`。唯一约束是多实例时的最后一道防线。

订单行有 `@Version`。锁失效时，后写的事务会得到 409，而不是静默覆盖。

创建订单不走机器。新单的状态就是初始状态 `CREATED`。没有“从空状态创建”这次事件，避免为了一张还不存在的订单去启动机器。

## 第 9 步：超时关单变成一次普通事件

内存里的 `timerOnce` 挂在状态机实例上。本模块的实例在请求结束就 `stop`，定时器会一起没掉。所以超时由 `PayTimeoutScanner` 扫描 `CREATED`、`PAYING`、`PAY_FAILED`，对更新时间早于阈值的订单发送 `TIMEOUT`。

关单仍然经过守卫之外的那条转移，流水里的操作者是 `system`，事件号是 `timeout-{orderNo}`，重复扫描不会关第二次。

支付窗口守卫和扫描器都读 `AppClock`。生产路径使用当前时间。测试可以冻结时间，不用真的等 30 分钟。

## 第 10 步：慢的外部工作离开状态机线程

这一步的细节和收益在 [05-persistence-and-threads.md](05-persistence-and-threads.md)。这里只标出挂载点：

- 仓库、退款、完成通知在对应动作里登记，提交后由 `stateMachineExecutor` 执行。
- 下单前的风控和库存查询走 `ParallelPrecheckService`，两路同时进行。预检结果只给页面提示，不作为转移依据，避免用过期库存做决定。
- 同一订单的事件在 `KeyStripedLock` 上串行，不同订单并行。

到这一步，一个支付成功请求的路径是：

```text
TradeOrderController
  -> TradeOrderAppService 按订单加锁
    -> TradeOrderFlowService 开启事务
      -> OrderMachineTemplate 恢复、发送 PAY_SUCCESS
        -> MarkPaidAction 写支付时间
        -> HighRiskGuard 选择下一状态
      -> 回写 status，插入流水
    -> 提交
  -> 释放锁
AfterCommitExecutor 在提交之后通知仓库或退款渠道
```

新增一种终态或一种审核，改配置、目录和必要的守卫或动作。流程服务的结构不用重写。
