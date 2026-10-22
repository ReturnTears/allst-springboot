# 16. 请求头

调用方身份、链路、幂等和操作人放在 Header。业务字段放在 Query 或 JSON Body。所有接口用同一个拦截器解析，控制器不再散落 `getHeader`。

## 头一览

| 头 | 必填 | 参与签名 | 作用 |
| --- | --- | --- | --- |
| `X-Trace-Id` | 否 | 否 | 链路号。不传则服务器生成，并在响应头回传 |
| `X-App-Id` | 签名接口必填 | 是 | 调用方应用标识，对应 `norm.sign.apps` |
| `X-Timestamp` | 签名接口必填 | 是 | 毫秒时间戳 |
| `X-Nonce` | 签名接口必填 | 是 | 一次性随机串 |
| `X-Sign` | 签名接口必填 | 是签名结果 | HMAC-SHA256 小写十六进制 |
| `X-Idempotent-Key` | 幂等接口必填 | 是 | 8 到 64 位，见幂等文档 |
| `X-Operator` | 否 | 是 | 操作人，写入 `createdBy`。空则存 `anonymous` |

常量在 `HeaderNames`。`HeaderContextInterceptor` 把这些值放进 `RequestContext`，请求结束时由 `RequestAuditFilter` 清理线程变量。

`OpenApiConfig` 会把上述头写进每个接口的文档。签名接口和幂等接口的必填状态根据注解自动判断。

业务代码读取操作人时用 `Operators.normalize(RequestContext.get().getOperator())`，长度超过 64 会被截断后再落库。
