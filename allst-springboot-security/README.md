# allst-springboot-security

这个模块把开发里常见的安全要求做成可以启动的 Spring Security 示例。默认用内嵌 H2，不准备数据库也能跑。每条规范的类名、配置和调用方式写在 `docs` 目录。

## 九项规范

| 编号 | 规范 | 文档 |
| --- | --- | --- |
| 1 | 认证 | [docs/01-authentication.md](docs/01-authentication.md) |
| 2 | 授权 | [docs/02-authorization.md](docs/02-authorization.md) |
| 3 | 攻击防护 | [docs/03-attack-protection.md](docs/03-attack-protection.md) |
| 4 | 会话管理 | [docs/04-session-management.md](docs/04-session-management.md) |
| 5 | 密码管理 | [docs/05-password-management.md](docs/05-password-management.md) |
| 6 | 协议集成 | [docs/06-protocol-integration.md](docs/06-protocol-integration.md) |
| 7 | 方法级安全 | [docs/07-method-security.md](docs/07-method-security.md) |
| 8 | 可扩展架构 | [docs/08-extensible-architecture.md](docs/08-extensible-architecture.md) |
| 9 | JWT 认证 | [docs/09-jwt.md](docs/09-jwt.md) |

索引也可以从 [docs/README.md](docs/README.md) 进入。

## 启动

在本模块目录执行：

```text
mvn spring-boot:run
```

服务端口 `1235`。浏览器访问 `http://localhost:1235/hello/security` 应返回 `Hello security`，这个地址故意不登录。

切换数据库：

```text
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=mysql
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=postgres
```

MySQL 库名 `allst_security`，PostgreSQL 同样。表可以由 JPA 在启动时补齐。若要自己建表，执行 `src/main/resources/db/schema-mysql.sql`，并把 `ddl-auto` 改成 `validate`。

前端控制台在 `frontend`，用 Vue 3。它通过 Vite 把 `/api`、`/session`、`/hello` 代理到 `1235`，这样会话 Cookie 和页面在同一个源上。

```text
cd frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。

## 示例账号

空库第一次启动会写入三个账号，口令只适合本机：

| 用户名 | 口令 | 角色 | 能做的事 |
| --- | --- | --- | --- |
| admin | Manage@123 | ROLE_ADMIN | 用户维护、审计、在线会话 |
| alice | Reader@123 | ROLE_USER | 查看用户、修改自己的密码 |
| auditor | Audit@123 | ROLE_AUDITOR | 查看用户和登录审计 |

`ROLE_ADMIN` 在表达式里继承 `ROLE_USER` 和 `ROLE_AUDITOR`。`@Secured` 和 `@RolesAllowed` 不吃这层继承，方法演示接口把这个差别留着。

## 持久化

用的是 Spring Data JPA。查询都走参数绑定，不拼接 SQL。H2 控制台关闭。
