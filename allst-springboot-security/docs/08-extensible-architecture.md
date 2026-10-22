# 8. 可扩展架构

新增一种检查或一种令牌声明时，加一个 Bean，不改原来的登录和签发代码。

## 三个扩展点

| 接口 | 何时调用 | 现成实现 |
| --- | --- | --- |
| `LoginGuard` | 所有登录入口读请求体之前 | `MaintenanceLoginGuard` |
| `TokenCustomizer` | 签发访问令牌之前 | `PasswordExpiredClaimCustomizer` |
| `SecurityProtocol` | 目录展示，以及你自己增加的过滤链 | `FormPasswordProtocol` 等 |

`LoginGuard.reject` 返回原因时，请求在认证之前结束。维护开关就是这样接上的。

`TokenCustomizer.customize` 收到一个可变的声明表和当前用户。密码是否过期被放进 `passwordExpired`。授权仍然以数据库里的角色为准，令牌里的声明只做提示。

新的过滤链用一个带 `@Order` 的 `SecurityFilterChain` Bean。数字小的先匹配。现有顺序：

| 顺序 | 链 | 路径 |
| --- | --- | --- |
| 0 | OAuth2，仅在启用时 | `/oauth2/**`、`/login/oauth2/**` |
| 1 | JWT | `/api/**` |
| 2 | HTTP Basic | `/basic/**` |
| 3 | 会话 | `/session/**` |
| 4 | 公开兜底 | `/hello/**`、`/error`，其余拒绝 |

兜底链存在，是因为自定义了 `SecurityFilterChain` 之后，Spring Boot 自带的默认登录链会整个退出。没有兜底时，没被前面匹配到的地址会裸奔。

## 运行时目录

```http
GET /api/security/modules
Authorization: Bearer <accessToken>
```

返回当前容器里的协议、`LoginGuard`、`TokenCustomizer` 和过滤链名字。类名以 Spring 代理结尾时会带上 `$$`，看简单类名即可对上源码。
