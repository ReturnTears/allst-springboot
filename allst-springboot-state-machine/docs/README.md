# 规范说明索引

每篇只讲一件事，并写明对应的类。改代码时以这里的类名为准回溯。

| 编号 | 主题 | 文档 | 主要代码 |
| --- | --- | --- | --- |
| 1 | 简化前的痛点 | [01-pain-and-target.md](01-pain-and-target.md) | 对照 `TradeOrderFlowService` 阅读 |
| 2 | 一步步收进状态机 | [02-simplify-steps.md](02-simplify-steps.md) | `TradeOrderStateMachineConfig`、`OrderMachineTemplate` |
| 3 | 订单状态图和职责 | [03-trade-order.md](03-trade-order.md) | `OrderState`、`OrderEvent`、守卫和动作 |
| 4 | 设计模式 | [04-design-patterns.md](04-design-patterns.md) | 见该篇表格 |
| 5 | 持久化与多线程 | [05-persistence-and-threads.md](05-persistence-and-threads.md) | `KeyStripedLock`、`AfterCommitExecutor`、`ParallelPrecheckService` |
| 6 | 合同并行审核 | [06-contract-regions.md](06-contract-regions.md) | `ContractStateMachineConfig` |
| 7 | 发票与选型 | [07-invoice-cola.md](07-invoice-cola.md) | `InvoiceMachineFactory` |
| 8 | 约定和踩坑 | [08-conventions.md](08-conventions.md) | `OrderMachineTemplate`、`ReserveStockGuard` |
| 9 | 接口演练 | [09-api.md](09-api.md) | 三个 Controller |

总览和启动方式见项目根目录 [README.md](../README.md)。
