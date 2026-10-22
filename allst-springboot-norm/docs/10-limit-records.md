# 10. 限制记录条数

单次查询和单次批量都有上限，避免一次拉取或一次写入把接口拖到超时。

## 配置

```yaml
norm:
  page:
    max-size: 50
    default-size: 10
    max-page: 1000
  batch:
    max-size: 100
```

| 限制 | 位置 | 超过时 |
| --- | --- | --- |
| 每页条数 | `@MaxPageSize` 和 `SizeGuard#pageSize` | HTTP 400，提示每页条数范围 |
| 页码 | `SizeGuard#pageIndex` | HTTP 400，业务码 `40010` |
| 批量条数 | `@BatchLimited` 和 `SizeGuard#batch` | HTTP 400，提示批量上限，业务码可能是校验码 `400` 或 `40011` |
| 请求体 | `RequestAuditFilter` | HTTP 413，业务码 `41301` |

页码从 0 开始。`max-page` 用来限制深分页的偏移量。调用方要导出全量数据时走专门的导出任务，而不是把 `size` 调到很大。

校验注解读取的是同一份配置，改 `application.yml` 后上限跟着变。`SizeGuard` 放在服务层，控制器以外的调用也会被拦住。

`GET /api/v1/users?page=0&size=51` 会失败。`POST /api/v1/users/batch` 超过 100 条会失败，并且不会插入任何一行。
