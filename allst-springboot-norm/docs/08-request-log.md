# 8. 请求日志

每次 `/api` 请求留下方法、路径、IP、状态码、耗时和截断后的报文，方便按 `traceId` 回溯。

## 实现

| 类 | 作用 |
| --- | --- |
| `RequestAuditFilter` | 生成或透传链路号，请求结束后记日志 |
| `ApiLogService` | `@Async` 落库，失败只打警告，不影响接口 |
| `ApiRequestLog` | 表 `norm_api_log` |
| `SensitiveTextMasker` | 日志正文里的手机号、身份证号打码并截断 |

响应头回传 `X-Trace-Id`。调用方传入则沿用，否则服务器生成 32 位无横线 UUID。日志格式里有 `[%X{traceId}]`。

配置：

```yaml
norm:
  access-log:
    persist: true
    max-body-length: 2000
    exclude-paths:
      - /api/health
```

`/api/health` 仍打印一行应用日志，不写表，避免压测把数据库打满。请求体按 `max-body-length` 截断，响应体截断到 500 字符。截断后缀算在长度里面，避免超出列宽。

异步接口会等结果写完再记状态码，所以日志里的状态是最终状态，不是「已开始异步」。

## 生产注意

- 高流量时把日志采集交给日志系统，数据库表只保留需要检索的摘要，并按 `created_at` 清理。
- 压测前把 `com.allst.springboot.web.RequestAuditFilter` 调到 WARN，否则写日志会变成瓶颈。
- 报文里的密码、令牌如果以后出现，要在 `SensitiveTextMasker` 里补规则，不能只依赖手机号正则。
