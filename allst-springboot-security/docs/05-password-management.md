# 5. 密码管理

口令只存 BCrypt 摘要。改密时检查复杂度、是否和用户名相同、是否落在禁用列表、是否和最近几次相同。

## 实现

| 类 | 作用 |
| --- | --- |
| `PasswordPolicy` | 长度 8 到 72，必须含大小写、数字和符号 |
| `PasswordService` | 本人改密、管理员重置、历史摘要 |
| `PasswordController` | `GET /api/password/policy`、`POST /api/password/change` |
| `PasswordExpiryFilter` | 密码过期后只放行查看自己、退出和改密 |
| `sec_password_history` | 最近使用过的摘要 |

配置在 `security.password`：

- `history-size`：保留几条历史，默认 5
- `max-age-days`：密码有效天数，默认 90。设为 0 表示不检查
- `denylist`：禁用口令

过期判断看 `password_changed_at`，不把账号标成无法登录。登录仍然发令牌，但除了改密相关接口，其余返回 `40310`。改密成功后，当前访问令牌和该用户全部刷新令牌作废，响应里给一对新令牌。

没有令牌、但还记得旧口令时，可以：

```http
POST /api/auth/password-expired
Content-Type: application/json

{"username":"alice","oldPassword":"Reader@123","newPassword":"Reader@456"}
```

用户不存在和旧口令错误都返回 `40101`。

管理员重置：

```http
PUT /api/users/{id}/password
Authorization: Bearer <具备 user:write 的令牌>

{"newPassword":"Reader@456"}
```

BCrypt 成本因子是 10。摘要比较用 `PasswordEncoder.matches`，不能拿明文和摘要做字符串相等。
