# 18. 职责单一

一个接口只做一件事，查询和写入分开，资料变更和状态变更分开。以后改分页条件时，不需要同时改创建逻辑。

## 划分

| 类型 | 类 | 只做什么 |
| --- | --- | --- |
| 用户查询 | `UserQueryController`、`UserQueryService` | 分页和按编号查询 |
| 用户写入 | `UserCommandController`、`UserCommandService` | 创建、改资料、改状态、删除、批量 |
| 订单查询 | `OrderQueryController`、`OrderQueryService` | 分页和按编号查询 |
| 订单写入 | `OrderCommandController`、`OrderCommandService` | 创建、批量创建、取消 |
| 通知 | `NotifyService` | 事务提交后的异步通知 |
| 统计 | `ReportController`、`ReportService` | 汇总计数 |

`PUT /api/v1/users/{id}` 只改姓名、手机号、邮箱、身份证号，不改用户名，也不改启用状态。启用和停用走 `PATCH /api/v1/users/{id}/status`。

`PATCH /api/v1/orders/{id}/cancel` 只负责取消。创建订单的接口不会顺便把状态改成已取消。

查询服务是只读事务。写入服务按方法开事务。返回给控制器的是 VO，不是实体，序列化和脱敏不依赖持久化会话。`spring.jpa.open-in-view` 设为 `false`，避免控制器里意外触发查询。

新增能力时加新的方法和新的 Command，而不是往一个「万能保存」接口上叠加可选字段。
