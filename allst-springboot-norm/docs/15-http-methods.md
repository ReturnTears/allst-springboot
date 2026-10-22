# 15. 请求方式

用 HTTP 方法表达动作。查询不改数据，创建、覆盖、局部更新和删除分开。

| 方法 | 含义 | 本项目中的接口 |
| --- | --- | --- |
| GET | 查询，不改数据 | `/api/health`，`/api/v1/users`，`/api/v1/users/{id}`，`/api/v1/orders`，`/api/v1/orders/{id}`，`/api/v1/reports/summary`，`/api/v1/demo/ping` |
| POST | 创建 | `/api/v1/users`，`/api/v1/users/batch`，`/api/v1/orders`，`/api/v1/orders/batch` |
| PUT | 全量替换资料 | `/api/v1/users/{id}`，不改用户名和状态 |
| PATCH | 局部变更 | `/api/v1/users/{id}/status` 改启用状态，`/api/v1/orders/{id}/cancel` 取消订单 |
| DELETE | 删除 | `/api/v1/users/{id}`，`/api/v1/users/batch` |

创建成功返回 HTTP 201。重复取消订单仍返回当前订单，调用方可以安全重试。

方法不对时返回 HTTP 405，body.code 为 `405`，例如 `POST /api/health`。

批量删除使用 `DELETE` 加 JSON body。部分 HTTP 客户端会丢掉 DELETE 的 body，对接时要确认客户端支持；不支持时再增加一个明确的 POST 动作，不要把删除塞进查询接口。
