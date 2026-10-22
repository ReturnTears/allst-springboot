# 4. 会话管理

给浏览器一条基于 Cookie 的链，和 JWT 那条链分开。会话链只处理 `/session/**`。

## 实现

| 类 | 作用 |
| --- | --- |
| `WebSecurityConfig#sessionChain` | 会话固定防护、并发会话、CSRF Cookie |
| `LoginSessionStrategy` | 登录成功后更换会话号，并登记到 `SessionRegistry` |
| `SessionController` | 读取 CSRF、当前人和在线会话 |
| `HttpSessionEventPublisher` | 会话销毁时从注册表移除 |

行为：

- 登录成功会 `changeSessionId`，避免沿用调用方事先拿到的会话号。
- 同一账号最多 1 个会话，新的登录让旧会话失效。
- 会话超时 30 分钟，Cookie 为 HttpOnly、SameSite=Lax。
- CSRF 令牌放在可读的 `XSRF-TOKEN` Cookie 里，提交时放到头 `X-XSRF-TOKEN`。

`GET /session/active` 只给管理员，并且只返回会话号前 8 位。

## 本地怎么调

登录是表单，不是 JSON：

```http
GET /session/csrf
```

从响应 Cookie 取出 `XSRF-TOKEN`，再：

```http
POST /session/login
Content-Type: application/x-www-form-urlencoded
X-XSRF-TOKEN: <token>

username=alice&password=Reader@123
```

然后带同一个会话 Cookie：

```http
GET /session/me
POST /session/logout
```

退出同样要带 CSRF 头。

前端开发时用 Vite 代理，让页面和接口看起来在同一个源，浏览器才会在请求里带上这个 Cookie。

## 失败码

| code | 含义 |
| --- | --- |
| 40105 | 会话失效，或已在别处登录 |
| 40100 | 未登录 |
