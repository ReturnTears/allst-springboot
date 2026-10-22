# 6. 协议集成

一种登录方式对应一个 `SecurityProtocol`。目录接口把当前装载的协议列出来。真正生效的，还要有自己的过滤链或入口。

## 已经接通

| 协议 | 类 | 入口 |
| --- | --- | --- |
| 用户名口令 | `FormPasswordProtocol` | `POST /api/auth/login` |
| JWT Bearer | `JwtBearerProtocol` | `Authorization: Bearer` |
| HTTP Basic | `HttpBasicProtocol` | `GET /basic/me` |
| 会话 Cookie | `SessionCookieProtocol` | `POST /session/login` |

HTTP Basic 不建立会话，每次请求都重新认证，所以每次成功都会多一条登录审计。

```http
GET /basic/me
Authorization: Basic <base64(auditor:Audit@123)>
```

## 条件启用

| 协议 | 类 | 开关 |
| --- | --- | --- |
| OAuth2 / OIDC 授权码 | `OAuth2ClientSecurityConfig` | `security.oauth2.enabled=true`，并提供客户端注册信息，示例见 `application-oauth2.yml` |
| LDAP 绑定 | `LdapBindClient` | `--spring.profiles.active=ldap`，内嵌目录见 `ldap/users.ldif` |

LDAP 只证明目录里的口令。绑定成功后按**同名本地账号**发 JWT，授权仍走本地角色。这样目录组和本系统权限不会混在一起。`LdapBindClient` 没有注册成全局 `AuthenticationProvider`，避免本地口令失败后又去目录上试一次。

```http
POST /api/auth/ldap
Content-Type: application/json

{"username":"alice","password":"Alice@123"}
```

未启用时返回 `40400`。

OAuth2 回调成功后，用身份提供方给出的用户名找本地账号。对不上返回 `40106`。提供方的用户名属性在 `application-oauth2.yml` 的 `user-name-attribute`。

## 预留

`Saml2Protocol` 只出现在目录里，当前不会注册 SAML 过滤链。接入时再增加 `spring-security-saml2-service-provider`、身份提供方元数据和一条 `SecurityFilterChain`。

## 查看目录

```http
GET /api/security/modules
Authorization: Bearer <accessToken>
```

`protocols` 数组里的 `enabled` 表示这条协议现在是否会参与登录。
