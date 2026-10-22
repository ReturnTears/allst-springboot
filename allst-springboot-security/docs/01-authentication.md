# 1. 认证

确认“你是谁”。本模块用用户名和口令查出本地账号，再把角色和权限放进安全上下文。

## 实现

| 类 | 作用 |
| --- | --- |
| `com.allst.springboot.security.user.SecurityUserDetailsService` | 按用户名加载账号，锁定时间到了就解开 |
| `com.allst.springboot.security.user.SecurityUser` | 放进 `SecurityContext` 的主体，按用户名比较 |
| `com.allst.springboot.web.AuthController` | `POST /api/auth/login` 校验口令并签发 JWT |
| `com.allst.springboot.security.auth.AuthenticationAuditListener` | 把成败写入 `sec_login_audit`，不记口令 |
| `com.allst.springboot.bootstrap.SecurityDataInitializer` | 空库时写入三个示例账号 |

口令校验使用 `DaoAuthenticationProvider` 和 BCrypt。用户不存在和口令错误都返回 `40101`，响应文案都是“用户名或密码错误”。

账号被停用返回 `40104`。连续失败被锁定返回 `40102`，见攻击防护那一篇。

## 本地怎么调

```http
POST /api/auth/login
Content-Type: application/json

{"username":"alice","password":"Reader@123"}
```

成功时 `code` 为 0，`data.accessToken` 是访问令牌。接着：

```http
GET /api/auth/me
Authorization: Bearer <accessToken>
```

最近 50 条登录记录：

```http
GET /api/audit/logins
Authorization: Bearer <具备 audit:read 的令牌>
```

`auditor / Audit@123` 可以看，`alice` 会被拒绝。

## 失败码

| code | 含义 |
| --- | --- |
| 40100 | 没有登录或令牌无效 |
| 40101 | 用户名或口令错误 |
| 40102 | 账号锁定 |
| 40104 | 账号停用或过期 |
