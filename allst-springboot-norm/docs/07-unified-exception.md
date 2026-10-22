# 7. 统一异常

预期内的失败和未捕获的异常都变成 `Result`。响应里没有堆栈、SQL 和密钥。

## 实现

| 类 | 作用 |
| --- | --- |
| `BizException` | 业务失败，携带 `ResultCode` |
| `GlobalExceptionHandler` | 控制器和拦截器抛出的异常 |
| `ResultErrorController` | 没有匹配到接口时的 `/error` |
| `ResultWriter` | 过滤器阶段就需要写 JSON 时使用 |

业务代码抛 `new BizException(ResultCode.NOT_FOUND, "用户不存在")`。拦截器里的签名、白名单失败也走这个异常，由 `DispatcherServlet` 交给 `GlobalExceptionHandler`。

| 异常 | HTTP | body.code |
| --- | --- | --- |
| 参数校验 | 400 | 400 |
| 签名问题 | 401 | 40100–40104 |
| IP 拒绝 | 403 | 40301 |
| 资源不存在 | 404 | 40401 |
| 路径不存在 | 404 | 404 |
| 方法不支持 | 405 | 405 |
| 数据冲突、幂等冲突 | 409 | 40900–40903 |
| 请求体过大 | 413 | 41301 |
| 限流 | 429 | 42901 |
| 未捕获异常 | 500 | 500 |

未捕获异常只在服务端日志里打印堆栈。`server.error.include-stacktrace` 和 `include-message` 都设为 `never`，避免容器错误页把内部信息带出去。

用户名唯一约束冲突在业务里先查再抛 `40902`。兜底的 `DataIntegrityViolationException` 返回 `40900`，日志只记数据库消息，不返回给调用方。
