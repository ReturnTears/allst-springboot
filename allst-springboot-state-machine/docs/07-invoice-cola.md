# 补充场景：发票用轻量状态机

发票流程是一条扁的链：草稿、待审核、已开具、已驳回、已作废。没有父状态，没有两条并行轨道，也没有选择伪状态。

这种形状继续用 Spring Statemachine，会为每张发票付出“建实例、恢复、停止”的成本，换不到层级和区域。这里改用 COLA 的 `cola-component-statemachine`。

## 定义是无状态的

`InvoiceMachineFactory` 用建造者描述转移，例如：

```text
DRAFT --SUBMIT / 金额为正--> PENDING_REVIEW
PENDING_REVIEW --APPROVE--> ISSUED
PENDING_REVIEW --REJECT--> REJECTED
REJECTED --RESUBMIT--> PENDING_REVIEW
ISSUED --RED_FLUSH--> VOIDED
DRAFT 或 REJECTED --VOID--> VOIDED
```

`StateMachine.fireEvent(当前状态, 事件, 上下文)` 把当前状态当参数传进去，返回目标状态。机器内部不保存“现在是哪张发票”。因此这份定义是单例，多个请求可以同时调用。

`InvoiceFlowService` 拿到返回值后写 `sm_invoice_apply.status`。返回值如果等于原状态，视为没有转移。失败回调会抛出业务异常，说明当前状态不接受这个事件。

金额达到 10000 时，`APPROVE` 的动作要求上下文角色是 `FINANCE`，否则抛出明确原因，状态保持待审核。角色字段写在请求体里，只为了把守卫演示出来。生产环境的角色必须来自登录态。客户端自己传 `FINANCE` 不能当作权限设计。

## 和 Spring Statemachine 怎么选

| 问题 | 选 Spring Statemachine 2.5 | 选 COLA |
| --- | --- | --- |
| 要不要层级、选择伪状态、fork/join | 要。订单和合同用它 | 这些不是它的模型 |
| 机器要不要在内存里保持当前状态 | 要，所以必须按单据建实例并恢复 | 不要，状态在参数和数据库里 |
| 能不能多线程共用一份定义 | 运行实例不能 | 可以 |
| 转移失败时的 API | `sendEvent` 的返回值不可靠，要看状态是否变化 | `fireEvent` 返回新状态，失败走 fail callback |
| 和 Spring 事务的关系 | 动作跑在调用线程上，可以参加外层事务 | `perform` 同样跑在调用线程上 |

订单和合同留在 Spring Statemachine，是因为选择伪状态、售后层级、双区域汇合都是这个框架直接表达的。发票留在 COLA，是为了避免用重型实例去跑一条五态的链。

`GET /api/invoices/meta/plantuml` 返回 COLA 生成的 PlantUML。它适合贴进文档或渲染成图，不参与运行时判断。

发票同样走分段锁、事件号幂等和 `sm_transition_log`。轻量的是状态机实现，不是幂等和审计这些外围约定。
