# 6. 统一返回

调用方只解析一种 JSON 结构，成功和失败都带链路号。

## 结构

```json
{
  "code": 200,
  "message": "成功",
  "data": {},
  "traceId": "链路号",
  "timestamp": 1710000000000
}
```

| 字段 | 含义 |
| --- | --- |
| code | 业务码。成功固定 `200`，失败见 `ResultCode` |
| message | 给人看的说明 |
| data | 业务数据。删除成功时为 `null` |
| traceId | 与响应头 `X-Trace-Id` 相同 |
| timestamp | 服务器毫秒时间戳 |

分页数据放在 `data` 里，类型是 `PageData`：`total`、`page`、`size`、`records`。`page` 从 0 开始，`size` 是本次实际使用的每页条数。

HTTP 状态和 body.code 同时存在。创建成功是 HTTP 201，body.code 仍是 200。签名失败是 HTTP 401，body.code 是 `40100` 到 `40104`。网关看 HTTP 状态，调用方分支看 body.code。

写接口时返回 `Result.ok(data)` 或 `ResponseEntity`。不要再返回裸字符串或各自定义的 Map。`HelloController` 是项目最初的示例，保留原样，新接口都走 `/api`。
