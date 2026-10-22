# 规范说明索引

每条规范单独一篇，对应的类和配置都写在文内。代码改动后，以这里的类名为准回溯。

| 编号 | 规范 | 文档 | 主要代码 |
| --- | --- | --- | --- |
| 1 | 签名，防止报文被篡改 | [01-signature.md](01-signature.md) | `SignatureInterceptor`、`SignUtils` |
| 2 | 加密，保护重要数据 | [02-encryption.md](02-encryption.md) | `AesCipher`、`AesStringConverter` |
| 3 | IP 白名单 | [03-ip-whitelist.md](03-ip-whitelist.md) | `IpWhitelistInterceptor` |
| 4 | 限流 | [04-rate-limit.md](04-rate-limit.md) | `RateLimit`、`SlidingWindowLimiter` |
| 5 | 参数校验 | [05-validation.md](05-validation.md) | `javax.validation`、`GlobalExceptionHandler` |
| 6 | 统一返回 | [06-unified-response.md](06-unified-response.md) | `Result`、`ResultCode` |
| 7 | 统一异常 | [07-unified-exception.md](07-unified-exception.md) | `BizException`、`GlobalExceptionHandler` |
| 8 | 请求日志 | [08-request-log.md](08-request-log.md) | `RequestAuditFilter`、`norm_api_log` |
| 9 | 幂等 | [09-idempotent.md](09-idempotent.md) | `Idempotent`、`IdempotentAspect` |
| 10 | 限制记录条数 | [10-limit-records.md](10-limit-records.md) | `SizeGuard`、`MaxPageSize` |
| 11 | 压测 | [11-stress-test.md](11-stress-test.md) | `ApiStressMain` |
| 12 | 异步处理 | [12-async.md](12-async.md) | `AsyncConfig`、`NotifyService`、`ReportService` |
| 13 | 数据脱敏 | [13-desensitize.md](13-desensitize.md) | `Desensitize`、`DesensitizeUtils` |
| 14 | 接口文档 | [14-api-doc.md](14-api-doc.md) | `OpenApiConfig`、Knife4j |
| 15 | 请求方式 | [15-http-methods.md](15-http-methods.md) | 各 Controller 的方法注解 |
| 16 | 请求头 | [16-request-headers.md](16-request-headers.md) | `HeaderNames`、`HeaderContextInterceptor` |
| 17 | 批量操作 | [17-batch.md](17-batch.md) | `UserCommandService#batchCreate` |
| 18 | 职责单一 | [18-single-responsibility.md](18-single-responsibility.md) | 查询/写入分控制器、分服务 |

一条业务请求会按顺序经过其中的多条规范。总览和启动方式见项目根目录 `README.md`。
