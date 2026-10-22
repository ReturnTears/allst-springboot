# 5. 参数校验

非法请求在进入事务和数据库之前被拒绝，减少无效占用。

## 实现

校验注解放在 Command 上，控制器方法参数加 `@Valid`。查询对象同样加 `@Valid`，页码和每页条数走同一套校验。

主要入参：

| 类 | 校验内容 |
| --- | --- |
| `UserCreateCommand` | 用户名、姓名、手机号、邮箱、身份证号 |
| `UserUpdateCommand` | 资料字段，不含用户名和状态 |
| `UserStatusCommand` | 状态只能是 0 或 1 |
| `UserPageQuery` / `OrderPageQuery` | 页码和每页条数 |
| `OrderCreateCommand` | 用户编号、金额精度 |
| `UserBatchCreateCommand` | 列表非空、条数上限、元素逐条校验 |

`GlobalExceptionHandler` 把 `MethodArgumentNotValidException`、`BindException`、`ConstraintViolationException` 收成 HTTP 400，body.code 为 `400`。消息形如 `username: 用户名不能为空; phone: 手机号格式不正确`。

JSON 无法解析时 code 为 `400`，消息是「请求体不是合法的 JSON」。请求体超过 `norm.http.max-body-bytes`（默认 1MB）时，过滤器直接返回 `41301`，不再把报文读进业务逻辑。

分页和批量的上限还在 `SizeGuard` 里再查一次，避免内部调用绕过注解。见 [10-limit-records.md](10-limit-records.md)。

## 示例

`POST /api/v1/users`，body 为 `{}`，签名合法时返回 400，消息中包含「用户名」。
