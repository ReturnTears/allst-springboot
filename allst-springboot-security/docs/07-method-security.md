# 7. 方法级安全

URL 只说明“这个地址要不要登录”。一个地址里面的不同动作，用方法注解分开。

## 实现

| 类 | 作用 |
| --- | --- |
| `MethodSecurityConfig` | 打开 `PreAuthorize`、`Secured`、`RolesAllowed`，并使用类代理 |
| `MethodDemoService` | 七种注解的示例 |
| `DomainPermissionEvaluator` | `hasPermission`：资源类型必须是 `user`，本人访问自己的编号时放行 |
| `MethodDemoController` | `/api/method/**` |

服务类没有接口，所以 `proxyTargetClass = true`。注解写在实现类的 public 方法上，并且要从别的 Bean 调用。同类内部自己调用不会经过代理，注解不生效。

## 表达式和另外两种注解

`ROLE_ADMIN > ROLE_USER` 和 `ROLE_ADMIN > ROLE_AUDITOR` 只对 `hasRole` / `hasAuthority` 表达式生效。

因此：

| 接口 | admin | alice | auditor |
| --- | --- | --- | --- |
| `GET /api/method/admin` | 通过 | 403 | 403 |
| `GET /api/method/jsr250-user` | 403 | 通过 | 403 |
| `GET /api/method/secured-auditor` | 403 | 403 | 通过 |

后两个接口管理员失败，是因为 `@RolesAllowed` 和 `@Secured` 不做角色继承。业务接口要管理员也能调用时，用 `hasRole` 表达式，不要用这两种注解。

`GET /api/method/owner?username=alice`：返回值必须是当前用户名，或者调用者是管理员。

`POST /api/method/filter` 的请求体是用户名数组。非管理员只能留下自己的名字。

`GET /api/method/user/{id}` 使用 `hasPermission(#userId, 'user', 'read')`。
