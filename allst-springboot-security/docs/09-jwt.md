# 9. JWT 认证

`/api/**` 不使用会话。访问令牌自己带签名和过期时间，刷新令牌的编号存在数据库里，退出和改密时可以作废。

## 实现

| 类 | 作用 |
| --- | --- |
| `JwtTokenService` | HS256 签发、解析、轮换、注销 |
| `JwtAuthenticationFilter` | 读取 `Authorization: Bearer` |
| `PasswordExpiryFilter` | 紧跟在上一过滤器后面 |
| `sec_refresh_token` | 刷新令牌的 `jti`、过期时间和是否作废 |
| `sec_token_blacklist` | 已退出的访问令牌 `jti` |
| `TokenCleanupJob` | 每小时删掉过期行 |

密钥是 `security.jwt.secret`，至少 32 字节。生产环境放到环境变量，不要提交真实密钥。签发者是 `security.jwt.issuer`。访问令牌默认 30 分钟，刷新令牌默认 7 天。时钟偏差允许 30 秒。

令牌里有 `typ`。访问接口只接受 `access`，刷新接口只接受 `refresh`。角色也会写进访问令牌，但过滤器每次仍按用户名读数据库，所以停用账号或调整权限后，下一次请求就生效，不用等令牌过期。

## 本地怎么调

登录见认证那一篇。刷新：

```http
POST /api/auth/refresh
Content-Type: application/json

{"refreshToken":"<refreshToken>"}
```

旧的刷新令牌立即作废。旧的访问令牌仍然可以用到它自己过期，除非拿它调用退出。

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
Content-Type: application/json

{"refreshToken":"<refreshToken>"}
```

不传刷新令牌时，只把当前访问令牌放进注销名单。

前端把访问令牌放在内存，刷新令牌放在 `sessionStorage`。这是演示取舍：刷新页面还能恢复登录，但页面里的脚本读得到刷新令牌。正式环境应把刷新令牌放到 HttpOnly Cookie，并缩短有效期。
