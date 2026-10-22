# 2. 授权

认证通过之后，按角色和权限码决定能不能访问。角色管身份，权限码管动作，例如 `user:read`。

## 实现

| 类 | 作用 |
| --- | --- |
| `com.allst.springboot.domain.entity.SecRole` | 角色，编码带 `ROLE_` 前缀 |
| `com.allst.springboot.domain.entity.SecPermission` | 权限码 |
| `com.allst.springboot.config.WebSecurityConfig` | URL 规则 |
| `com.allst.springboot.config.MethodSecurityConfig` | 角色继承：`ROLE_ADMIN` 包含审计员和普通用户 |
| `com.allst.springboot.security.rbac.UserAdminService` | 方法上再拦一层，避免内部调用绕过 URL |

URL 和方法注解是两道门。URL 挡住从外部进来的请求，方法注解挡住同一个进程里的其他调用。

当前 URL 规则：

| 路径 | 要求 |
| --- | --- |
| `GET /api/users` | `user:read` |
| `POST /api/users`、`PUT /api/users/**` | `user:write` |
| `GET /api/audit/**` | `audit:read` |
| `GET /session/active` | `ROLE_ADMIN` |
| `GET /api/method/**` | 已登录，细粒度在方法上 |

`hasRole('ADMIN')` 会走角色继承。`hasAuthority('user:read')` 不走继承，所以管理员的权限是初始化时写进 `sec_role_permission` 的，不是继承来的。

## 本地怎么调

用 `admin / Manage@123` 登录后：

```http
GET /api/users
Authorization: Bearer <accessToken>
```

创建用户：

```http
POST /api/users
Authorization: Bearer <accessToken>
Content-Type: application/json

{"username":"bob","password":"Reader@123","displayName":"鲍勃","roleCodes":["ROLE_USER"]}
```

`alice` 调用上面的 POST 会得到 `40300`。停用自己会被服务拒绝，避免把当前管理员锁在外面。

## 失败码

| code | 含义 |
| --- | --- |
| 40300 | 已登录但权限不够 |
| 40002 | 用户名已存在 |
