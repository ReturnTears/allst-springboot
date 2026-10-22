# 规范说明索引

每条规范单独一篇。代码改动后，以文内的类名为准回溯。

| 编号 | 规范 | 文档 | 主要代码 |
| --- | --- | --- | --- |
| 1 | 认证 | [01-authentication.md](01-authentication.md) | `SecurityUserDetailsService`、`AuthController` |
| 2 | 授权 | [02-authorization.md](02-authorization.md) | `WebSecurityConfig`、`UserAdminService` |
| 3 | 攻击防护 | [03-attack-protection.md](03-attack-protection.md) | `LoginProtectionFilter`、`LoginAttemptService` |
| 4 | 会话管理 | [04-session-management.md](04-session-management.md) | `LoginSessionStrategy`、`SessionController` |
| 5 | 密码管理 | [05-password-management.md](05-password-management.md) | `PasswordPolicy`、`PasswordService` |
| 6 | 协议集成 | [06-protocol-integration.md](06-protocol-integration.md) | `SecurityProtocol`、`BasicDemoController` |
| 7 | 方法级安全 | [07-method-security.md](07-method-security.md) | `MethodSecurityConfig`、`MethodDemoService` |
| 8 | 可扩展架构 | [08-extensible-architecture.md](08-extensible-architecture.md) | `LoginGuard`、`TokenCustomizer`、`SecurityCatalog` |
| 9 | JWT 认证 | [09-jwt.md](09-jwt.md) | `JwtTokenService`、`JwtAuthenticationFilter` |

启动方式和示例账号见项目根目录 `README.md`。
