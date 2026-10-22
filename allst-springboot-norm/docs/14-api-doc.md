# 14. 接口文档

文档从代码注解生成，调用方以运行中的文档为准，减少口头对齐。

## 地址

应用启动后：

| 地址 | 说明 |
| --- | --- |
| http://127.0.0.1:1234/doc.html | Knife4j |
| http://127.0.0.1:1234/swagger-ui.html | Swagger UI |
| http://127.0.0.1:1234/v3/api-docs | OpenAPI JSON，可导入 Postman |

实现在 `OpenApiConfig`。每个操作都会带上公共请求头；需要签名的接口把 `X-App-Id`、`X-Timestamp`、`X-Nonce`、`X-Sign` 标成必填，带 `@Idempotent` 的接口再把 `X-Idempotent-Key` 标成必填。

控制器使用 `@Tag`，方法使用 `@Operation`，入参使用 `@Schema`。新增接口时补这三项，文档才会完整。

## 分组

文档里的标签和代码职责一致：用户查询、用户写入、订单查询、订单写入、限流示例、异步报表、健康检查、开发辅助。

开发辅助分组在生产必须消失。设置：

```yaml
norm:
  dev-tools-enabled: false
knife4j:
  production: true
```

`knife4j.production=true` 会关闭文档页面。对外部开放的环境不要保留 Swagger。

## 对接顺序

1. 从文档确认路径、方法和字段。
2. 用 [01-signature.md](01-signature.md) 的算法或本地的 `/api/dev/sign` 生成签名。
3. 创建类接口准备幂等键。
4. 按 body.code 处理失败，按 HTTP 状态做网关告警。
