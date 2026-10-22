# 3. 攻击防护

这里只做防御：限制撞库、收紧响应头、避免把口令和多余错误细节返回给调用方。不提供攻击步骤。

## 实现

| 类或配置 | 作用 |
| --- | --- |
| `LoginProtectionFilter` | 登录入口的维护开关和按 IP 滑动窗口 |
| `SlidingWindowLimiter` | 单机窗口。多实例要换成共享存储，否则各算各的 |
| `LoginAttemptService` | 同一账号连续失败 5 次后锁定 15 分钟 |
| `MaintenanceLoginGuard` | `security.maintenance=true` 时拒绝登录 |
| `WebSecurityConfig#applyHeaders` | `X-Frame-Options: DENY`、`nosniff`、CSP、Referrer-Policy |
| `PasswordPolicy#checkDisplayName` | 显示名拒绝引号和尖括号 |
| JPA 参数查询 | 不拼接 SQL |

登录入口是：

- `POST /api/auth/login`
- `POST /api/auth/password-expired`
- `POST /api/auth/ldap`
- `POST /session/login`

默认每个 IP 每分钟 20 次，超过返回 `42900`。默认不读取 `X-Forwarded-For`。只有网关会覆盖这个头、并且 `security.login.trust-forwarded=true` 时才打开，否则调用方可以伪造地址躲开限流。

锁定只在时间窗结束后由下次登录自动解开，继续失败不会把锁定时间往后推。

`/api/**` 使用 Bearer，不靠 Cookie，所以关掉 CSRF。`/session/**` 保留 CSRF。H2 控制台关闭。错误响应不带堆栈。

`X-XSS-Protection` 仍写着，但现代浏览器已经不用这个响应头。真正靠的是 JSON 编码、Vue 的文本插值和 CSP。

## 失败码

| code | HTTP | 含义 |
| --- | --- | --- |
| 42900 | 429 | 登录过于频繁 |
| 50301 | 503 | 维护中，暂停登录 |
| 40102 | 401 | 账号已锁定 |
