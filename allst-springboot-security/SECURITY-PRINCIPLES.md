# allst-springboot-security 实现原理详解

> 本文是对模块 `allst-springboot-security` 的逐功能源码解读。每个知识点都按
> **「Spring Security 原理 → 本模块对应代码 → 代码在做什么 → 现实项目中的用法」**
> 四段展开，帮助理解"为什么这样写"，而不只是"写了什么"。
>
> 与 `docs/01~09` 的关系：那九篇是**按规范主题**写的操作说明（怎么配、怎么调）；
> 本文是**按实现原理**写的代码导读（哪段代码、为什么这么实现）。两者互补，
> 建议先读本文建立整体认识，再回到 `docs/` 查具体规范。
>
> 技术栈：Spring Boot 2.6.5 / Spring Security 5.6 / Java 11 / JJWT 0.11.5 / JPA + H2(默认)。
> 所有路径相对模块根目录 `allst-springboot-security/`，包根为 `com.allst.springboot`。

---

## 目录

- [0. 模块全景与请求处理总流程](#0-模块全景与请求处理总流程)
- [1. 安全过滤链：四条链的分工与匹配原理](#1-安全过滤链四条链的分工与匹配原理)
- [2. 认证体系：AuthenticationManager / Provider / UserDetailsService](#2-认证体系authenticationmanager--provider--userdetailsservice)
- [3. JWT 无状态认证：签发、校验、刷新、注销](#3-jwt-无状态认证签发校验刷新注销)
- [4. 授权体系：URL 规则、方法注解、角色继承、数据级权限](#4-授权体系url-规则方法注解角色继承数据级权限)
- [5. 攻击防护：限流、锁定、CSRF、安全响应头](#5-攻击防护限流锁定csrf安全响应头)
- [6. 会话管理：会话固定、并发控制、注册表](#6-会话管理会话固定并发控制注册表)
- [7. 密码管理：BCrypt、策略、历史、过期](#7-密码管理bcrypt策略历史过期)
- [8. 协议集成：Basic / OAuth2 / LDAP / SAML2](#8-协议集成basic--oauth2--ldap--saml2)
- [8.5 短信验证码登录（阿里云 + 自动注册）](#85-短信验证码登录阿里云--自动注册)
- [8.6 扫码登录（跨设备授权 + 轮询）](#86-扫码登录跨设备授权--轮询)
- [9. 可扩展架构：三个扩展点与运行时目录](#9-可扩展架构三个扩展点与运行时目录)
- [10. 统一异常与响应：401 / 403 / 业务码](#10-统一异常与响应401--403--业务码)
- [11. 关键设计取舍与常见疑问](#11-关键设计取舍与常见疑问)
- [附录 A：功能 → 代码文件速查表](#附录-a功能--代码文件速查表)
- [附录 B：一次完整请求的调用链](#附录-b一次完整请求的调用链)

---

## 0. 模块全景与请求处理总流程

### 0.1 这个模块在演示什么

模块把企业项目里常见的安全要求做成了**可启动的示例**，覆盖九项规范：
认证、授权、攻击防护、会话管理、密码管理、协议集成、方法级安全、可扩展架构、JWT。
它不是"一个登录功能"，而是"一套安全规范的参考实现"。

包结构（`src/main/java/com/allst/springboot/`）：

| 包 | 职责 | 关键类 |
| --- | --- | --- |
| `config` | 安全总配置、方法安全、OAuth2、LDAP、属性绑定 | `WebSecurityConfig`、`MethodSecurityConfig`、`SecurityProperties` |
| `security.user` | 认证主体与账号加载 | `SecurityUser`、`SecurityUserDetailsService`、`SecurityUsers` |
| `security.jwt` | JWT 签发/校验/过滤器/扩展点 | `JwtTokenService`、`JwtAuthenticationFilter`、`PasswordExpiryFilter` |
| `security.auth` | 登录防护、限流、锁定、审计、JSON 出入口 | `LoginProtectionFilter`、`LoginAttemptService`、`AuthenticationAuditListener` |
| `security.session` | 会话登录策略 | `LoginSessionStrategy` |
| `security.password` | 密码策略与改密 | `PasswordPolicy`、`PasswordService` |
| `security.method` | 方法级安全演示与数据权限 | `MethodDemoService`、`DomainPermissionEvaluator` |
| `security.rbac` | 用户/角色/权限管理 | `UserAdminService` |
| `security.protocol` | 协议目录项（仅展示） | `SecurityProtocol` 及 7 个实现 |
| `security.extension` | 运行时扩展清单 | `SecurityCatalog` |
| `web` | REST 控制器 | `AuthController`、`SessionController`、`PasswordController` 等 |
| `domain` | JPA 实体与仓库 | `SecUser`、`SecRole`、`SecPermission`、`RefreshToken`、`TokenBlacklist` |
| `bootstrap` | 空库种子数据 | `SecurityDataInitializer` |
| `common` | 统一响应/异常/IP 解析 | `ApiResult`、`GlobalExceptionHandler`、`ClientIpResolver` |

### 0.2 Spring Security 的核心思想：过滤链而非手写 if

Spring Security 不在每个接口里写 `if (没登录) return 401`，而是把请求先送进一条
**Servlet 过滤器链（SecurityFilterChain）**，由链上的过滤器依次完成：
认证 → 授权 → 异常处理 → 放行到控制器。

```
浏览器/客户端
   │  HTTP 请求
   ▼
Servlet 容器过滤器（如 LoginProtectionFilter，仅登录地址）
   ▼
Spring Security 过滤链（按 @Order 选中一条）
   ├─ 安全响应头过滤器
   ├─ CSRF 过滤器（会话链）
   ├─ 认证过滤器（JwtAuthenticationFilter / BasicAuthenticationFilter / 表单过滤器）
   ├─ 授权过滤器（FilterSecurityInterceptor，执行 URL 规则）
   └─ 异常翻译过滤器（ExceptionTranslationFilter → EntryPoint / DeniedHandler）
   ▼
DispatcherServlet → 控制器 → 服务方法（@PreAuthorize 在此生效）
```

**为什么这样设计**：认证授权是横切关注点，集中到链上后，业务代码不必关心"谁在调用、
有没有权限"，只需从 `SecurityContext` 取当前用户。新增接口时只要补一条 URL 规则，
不会漏写某个接口里的 if。

### 0.3 一次请求的完整生命周期（以 JWT 业务请求为例）

以 `GET /api/users`（带 `Authorization: Bearer xxx`）为例：

1. `LoginProtectionFilter` 只挂在四个登录地址上，本请求不经过它。
2. 进入 `@Order(1)` 的 `apiChain`（匹配 `/api/**`）。
3. `JwtAuthenticationFilter` 验签 → 查注销名单 → 按用户名读库 → 把 `SecurityUser`
   放进 `SecurityContextHolder`。
4. `PasswordExpiryFilter` 检查密码是否过期（本地址不在放行名单则拦截）。
5. `FilterSecurityInterceptor` 执行 URL 规则 `hasAuthority("user:read")`。
6. 通过则进入 `UserController.list` → `UserAdminService.list`，方法上再校验一次
   `@PreAuthorize("hasAuthority('user:read')")`。
7. 返回 JSON。任何一步失败都由对应的处理器写 401/403。

> 这条链路在 `WebSecurityConfig.apiChain` 的 Javadoc 里有原文描述，本文第 1、3、4 节逐段拆解。

---

## 1. 安全过滤链：四条链的分工与匹配原理

### 1.1 知识点：SecurityFilterChain 与 @Order

Spring Security 5.4+ 推荐用 `SecurityFilterChain` Bean 取代继承
`WebSecurityConfigurerAdapter`。每条链由两部分决定：

- **匹配范围**：`http.antMatcher("/api/**")` 或 `http.requestMatchers()`，决定"哪些请求归这条链管"。
- **处理顺序**：`@Order(n)`，数字小的先匹配。**一个请求只会被第一条匹配上的链处理**，
  后面的链不再看它。

只要容器里出现自定义 `SecurityFilterChain`，Spring Boot 默认的"所有地址都要登录、
跳 HTML 登录页"那套自动配置就退出，因此**必须自己把每条路径的放行/拒绝写全**。

### 1.2 对应代码

`config/WebSecurityConfig.java` 定义了四条链：

| 链 | @Order | 匹配 | 认证方式 | 会话策略 | 代码位置 |
| --- | --- | --- | --- | --- | --- |
| `apiChain` | 1 | `/api/**` | JWT Bearer | STATELESS | `WebSecurityConfig.java:201-237` |
| `basicChain` | 2 | `/basic/**` | HTTP Basic | STATELESS | `WebSecurityConfig.java:245-262` |
| `sessionChain` | 3 | `/session/**` | 表单 + Cookie | IF_REQUIRED | `WebSecurityConfig.java:280-328` |
| `publicChain` | 4 | `/**`（兜底） | 无 | 默认 | `WebSecurityConfig.java:335-349` |

另有条件链 `oauth2Chain`（`@Order(0)`，仅 `security.oauth2.enabled=true` 时注册），
见 `config/OAuth2ClientSecurityConfig.java:32-41`。

### 1.3 代码在做什么

**为什么拆成四条而不是写一条大链？** 因为登录方式不同，安全策略互相冲突：

- API 用 JWT，**不能建会话**（`STATELESS`），也**不需要 CSRF**（令牌在请求头里，
  浏览器不会自动携带，恶意网站无法替用户发请求）。
- 会话链用 Cookie，**必须开 CSRF**、**必须防会话固定**、**必须控制并发会话**。
- Basic 链每次请求都带用户名口令，无状态。

如果硬塞进一条链，`csrf().disable()` 会同时关掉会话链的 CSRF 保护，
`sessionManagement()` 也无法对 JWT 和 Cookie 分别设置。拆链让每种登录方式各用一套规则。

关键代码（`apiChain`，`WebSecurityConfig.java:206-235`）：

```java
http.antMatcher("/api/**");                 // 只接管 /api/** 的请求
http.csrf().disable();                      // Bearer 不靠 Cookie，CSRF 无意义
http.cors();                                // 启用下面 corsConfigurationSource 的跨域规则
http.requestCache().disable();              // 不记住"用户刚才想访问哪"，那会偷偷建会话
http.sessionManagement()
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS);  // 不读、不建会话
// ... URL 规则 ...
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
http.addFilterAfter(passwordExpiryFilter, JwtAuthenticationFilter.class);
```

**兜底链的"默认拒绝"**（`publicChain`，`WebSecurityConfig.java:345-347`）：

```java
http.authorizeRequests()
    .antMatchers("/hello/**", "/error").permitAll()  // 探活与错误页放行
    .anyRequest().denyAll();                          // 其余一律拒绝
```

这是**安全默认（secure by default）**：忘记配置的新接口不会变成匿名可访问，而是直接 403。
`/error` 放行是为了容器转发错误页时不再被拦一次。

### 1.4 现实项目中的用法

- 多端系统（App 用 JWT、管理后台用 Cookie、老系统用 Basic）就按本模块这样拆链。
- 单端项目通常只需一条链，但仍建议保留 `anyRequest().denyAll()` 或 `authenticated()` 兜底。
- `@Order` 冲突是常见坑：两条链都匹配 `/api/**` 时，只有数字小的生效，另一条的规则被"吃掉"。
  本模块用互斥的 `antMatcher` 前缀避免这个问题。

---

## 2. 认证体系：AuthenticationManager / Provider / UserDetailsService

### 2.1 知识点：认证的三层抽象

Spring Security 把"验证用户名口令"拆成三层，各司其职：

```
AuthenticationManager（入口，决定用哪个 Provider）
   └─ AuthenticationProvider（具体校验逻辑，如 DaoAuthenticationProvider）
         └─ UserDetailsService（按用户名加载账号）+ PasswordEncoder（比对口令）
```

- `AuthenticationManager.authenticate(token)` 是控制器调用的统一入口。
- `ProviderManager` 持有一组 `Provider`，逐个尝试，第一个支持该 token 类型的负责校验。
- `DaoAuthenticationProvider` 调 `UserDetailsService.loadUserByUsername` 取账号，
  再用 `PasswordEncoder.matches` 比对口令，最后检查账号状态（停用/锁定/过期）。

### 2.2 对应代码

| 角色 | 类 | 位置 |
| --- | --- | --- |
| 认证入口 | `WebSecurityConfig.authenticationManager` | `WebSecurityConfig.java:103-106` |
| 口令校验器 | `WebSecurityConfig.daoAuthenticationProvider` | `WebSecurityConfig.java:90-97` |
| 账号加载 | `SecurityUserDetailsService` | `security/user/SecurityUserDetailsService.java` |
| 认证主体 | `SecurityUser`（实现 `UserDetails`） | `security/user/SecurityUser.java` |
| 权限装配 | `SecurityUsers.authorities` | `security/user/SecurityUsers.java:23-36` |
| 登录编排 | `AuthController.login` | `web/AuthController.java:78-83` |

### 2.3 代码在做什么

**(1) 只挂一个 Provider，避免"口令错后再去 LDAP 试一次"**

```java
// WebSecurityConfig.java:103-106
@Bean
public AuthenticationManager authenticationManager(AuthenticationProvider daoAuthenticationProvider) {
    return new ProviderManager(daoAuthenticationProvider);
}
```

`ProviderManager` 只装了 `DaoAuthenticationProvider`。如果把 LDAP 的 Provider 也挂进来，
普通登录口令错误后框架会接着去目录上再试一次，本地账号和目录账号会搅在一起。
所以 LDAP 单独放在 `LdapBindClient`，由 `POST /api/auth/ldap` 显式调用（见第 8 节）。

**(2) 隐藏"用户不存在"，防账号探测**

```java
// WebSecurityConfig.java:95
provider.setHideUserNotFoundExceptions(true);
```

默认情况下"用户不存在"抛 `UsernameNotFoundException`、"口令错"抛 `BadCredentialsException`，
攻击者可凭返回文案差异探测哪些用户名已注册。设为 true 后两者都变成 `BadCredentialsException`，
对外统一显示"用户名或密码错误"。`GlobalExceptionHandler` 和 `JsonLoginFailureHandler`
也刻意不区分这两种失败。

**(3) UserDetailsService 里的事务与懒加载**

```java
// SecurityUserDetailsService.java:31-47
@Override
@Transactional
public UserDetails loadUserByUsername(String username) {
    SecUser user = users.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    unlockIfDue(user);
    return new SecurityUser(..., SecurityUsers.authorities(user));
}
```

- `@Transactional` 是必须的：`SecUser.roles` 是 `FetchType.LAZY`（`SecUser.java:68`），
  角色和权限要在事务内拷贝进 `SecurityUser`，事务外再遍历集合会抛 `LazyInitializationException`。
  这也是 `application.yml` 里 `open-in-view: false` 的配套要求——视图层不偷偷查库，
  安全代码需要的地方显式加事务。
- `unlockIfDue`（`SecurityUserDetailsService.java:49-59`）：锁定时间到了就在**下次登录时**
  顺手解锁，而不是另写定时任务。用户下次登录自然走到这里，`lockUntil` 过期即恢复。

**(4) 把"用户→角色→权限"摊平成 GrantedAuthority**

```java
// SecurityUsers.java:23-36
for (SecRole role : user.getRoles()) {
    codes.add(role.getCode());                 // ROLE_ADMIN
    for (SecPermission p : role.getPermissions()) {
        codes.add(p.getCode());                // user:read
    }
}
```

`hasRole('ADMIN')` 认的是 `ROLE_ADMIN`，`hasAuthority('user:read')` 认的是权限码，
两者放进同一个 `GrantedAuthority` 列表。这样 URL 规则和方法注解都能用。
角色继承（ADMIN 自动拥有 USER）不在这一步，而在表达式处理器里（见第 4 节）。

**(5) SecurityUser 的两个细节**

```java
// SecurityUser.java:44-48
@Override
public boolean isCredentialsNonExpired() {
    return true;   // 密码过期仍允许登录，改密限制交给 PasswordExpiryFilter
}
```

如果这里返回 false，密码过期的用户会**直接登录失败**，连改密接口都进不去。
所以固定返回 true，把"过期后只能改密"的限制放到 `PasswordExpiryFilter`（见第 7 节）。

```java
// SecurityUser.java:50-64  equals/hashCode 只看 username
```

会话注册表 `SessionRegistry` 用 `equals` 判断"这两次登录是不是同一个人"，
所以必须按用户名比较，而不是按对象引用。

### 2.4 现实项目中的用法

- 账号状态字段（`enabled`/`accountNonLocked`/`accountNonExpired`）直接映射 `UserDetails`
  的同名方法，框架会自动在认证时检查并抛对应异常，无需手写 if。
- 多认证源（本地 + LDAP + OAuth2）时，**不要**把所有 Provider 堆进一个 Manager，
  按登录入口分开，避免失败后误试其他源。
- 懒加载集合 + `open-in-view: false` 是 JPA 项目的标准组合，安全代码记得显式事务。

---

## 3. JWT 无状态认证：签发、校验、刷新、注销

### 3.1 知识点：JWT 与双令牌

JWT（JSON Web Token）是**自包含**的令牌：签名保证不可篡改，过期时间保证时效，
服务端不必存会话即可校验。本模块用 **HS256 对称签名**（JJWT 库），并采用**双令牌**：

- **访问令牌（access）**：短命（默认 30 分钟），给业务接口用，**不落库**，靠签名+过期校验。
- **刷新令牌（refresh）**：长命（默认 7 天），只给 `/api/auth/refresh` 用，
  **只把编号 jti 落库**（原始字符串不存），每次使用作废旧 jti（轮换）。

为什么访问令牌不落库？因为每次请求都查签发记录会拖慢无状态接口；签名+过期已足够。
代价是"退出前无法主动作废某一张"，所以用**注销名单（黑名单）**补这个缺口。

### 3.2 对应代码

| 功能 | 类/方法 | 位置 |
| --- | --- | --- |
| 签发/校验/刷新/注销 | `JwtTokenService` | `security/jwt/JwtTokenService.java` |
| 请求级校验过滤器 | `JwtAuthenticationFilter` | `security/jwt/JwtAuthenticationFilter.java` |
| 密码过期拦截 | `PasswordExpiryFilter` | `security/jwt/PasswordExpiryFilter.java` |
| 声明扩展点 | `TokenCustomizer` / `PasswordExpiredClaimCustomizer` | `security/jwt/` |
| 登录/刷新/退出入口 | `AuthController` | `web/AuthController.java` |
| 注销名单清理 | `TokenCleanupJob` | `job/TokenCleanupJob.java` |

### 3.3 代码在做什么

**(1) 启动即校验密钥长度**

```java
// JwtTokenService.java:65-72
@PostConstruct
public void init() {
    byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
    if (secret.length < 32) {
        throw new IllegalStateException("security.jwt.secret 长度至少 32 字节");
    }
    this.key = Keys.hmacShaKeyFor(secret);
}
```

HS256 要求密钥至少 32 字节。**启动时就失败**，避免带着过短密钥跑起来后所有令牌都能被伪造。
密钥来自 `application.yml` 的 `security.jwt.secret`，生产环境应换成随机值并只放环境变量。

**(2) 签发：写声明 → 扩展点 → 签名 → 刷新令牌落库**

```java
// JwtTokenService.java:79-110（节选）
claims.put("uid", user.getId());
claims.put("roles", ...ROLE_ 开头的权限...);
for (TokenCustomizer customizer : customizers) {
    customizer.customize(claims, user);     // 扩展点：签名前追加声明
}
String access = build(..., TYPE_ACCESS, ...);
String refresh = build(..., TYPE_REFRESH, ...);
refreshTokens.save(stored);                 // 只存 jti，不存原始字符串
```

- 角色写进令牌**只是给前端展示**；授权时过滤器仍会再查数据库（见下）。
- `TokenCustomizer` 是开闭原则的扩展点：要加声明就实现接口注册成 Bean，不必改签发方法。
  现有实现 `PasswordExpiredClaimCustomizer` 把 `passwordExpired` 写进令牌供登录页提示。

**(3) 校验：验签 → 核对签发者 → 允许时钟偏差**

```java
// JwtTokenService.java:113-121
return Jwts.parserBuilder()
        .setSigningKey(key)
        .requireIssuer(properties.getJwt().getIssuer())  // 防止拿别的系统签的令牌来用
        .setAllowedClockSkewSeconds(30)                  // 两台机器时间略差时不误判过期
        .build().parseClaimsJws(token).getBody();
```

**(4) 过滤器：每个 /api/** 请求都过，但无 Bearer 直接放行**

```java
// JwtAuthenticationFilter.java:44-70（节选）
if (header == null || !header.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);   // 登录/刷新等还没令牌的请求走这里
    return;
}
Claims claims = tokens.parse(...);
if (!TYPE_ACCESS.equals(claims.get("typ"))) { reject(...); }   // 刷新令牌不能当访问令牌
if (tokens.accessRevoked(claims.getId())) { reject(...); }     // 查注销名单
SecurityUser user = users.loadUserByUsername(claims.getSubject());  // 再查一次库
if (!user.isEnabled() || ...) { reject(...); }
SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
```

**最后一步"再查库"是故意的**：令牌里虽写了角色，但权限以数据库为准。
这样停用账号、改角色后，**下一次请求就生效**，不必等令牌过期。
`credentials` 传 null，避免把口令摘要再放进安全上下文。

**(5) 刷新：先作废旧 jti，再签新的一对**

```java
// JwtTokenService.java:127-145（节选）
RefreshToken stored = refreshTokens.findByJti(claims.getId())...;
if (stored.isRevoked() || 过期) { throw ...; }
stored.setRevoked(true);                  // 旧刷新令牌立刻作废
refreshTokens.save(stored);
SecurityUser user = users.loadUserByUsername(...);
if (!user.isEnabled() || ...) { throw ...; }  // 停用/锁定不能靠旧令牌续命
return issue(user);
```

轮换保证**一张刷新令牌只能用一次**，被偷走后第二次使用即失败。
中途失败用户需重新登录，这是为了不让同一张令牌被反复使用。

**(6) 注销：访问令牌进黑名单，刷新令牌置 revoked**

```java
// JwtTokenService.java:147-180
revokeAccess：把访问令牌 jti 写入 sec_token_blacklist，过滤器每次查这张表
revokeRefresh：把刷新令牌 jti 的 revoked 置 true
```

退出时令牌已损坏也算退出完成（吞掉 `JwtException`），不把异常抛给调用方。
`TokenCleanupJob`（`job/TokenCleanupJob.java:26-32`）每小时删过期记录，
但**未过期的注销记录不能删**——访问令牌过期前仍靠它判断"已退出"。

**(7) 过滤器只挂在 API 链，关掉 Servlet 自动注册**

```java
// WebSecurityConfig.java:153-158
FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
registration.setEnabled(false);   // 否则 Boot 会把它注册成对所有地址生效的 Servlet 过滤器
```

过滤器只要是 Spring Bean，Boot 还会把它注册成全局 Servlet 过滤器，导致每个请求被校验两次。
这里显式关掉自动注册，让它只待在 `apiChain` 里。`PasswordExpiryFilter` 同理。

### 3.4 现实项目中的用法

- 对称 HS256 适合单体；多服务验签应换 RS256（公钥验签、私钥签发）或交给授权服务器。
- 访问令牌短命 + 刷新令牌轮换 + 注销名单，是"无状态"与"可主动作废"之间的标准折中。
- 黑名单/刷新表会随用户量增长，记得像 `TokenCleanupJob` 一样定期清理过期记录。
- 多实例部署时，注销名单和刷新表必须放共享存储（本模块用 JPA 数据库，天然共享）。

---

## 4. 授权体系：URL 规则、方法注解、角色继承、数据级权限

### 4.1 知识点：四层授权

本模块的授权分四层，从粗到细：

1. **URL 规则**（过滤链）：`hasAuthority` / `hasRole` / `authenticated`，决定"能不能进这个地址"。
2. **方法注解**（AOP 代理）：`@PreAuthorize` / `@PostAuthorize` / `@PreFilter` / `@Secured` / `@RolesAllowed`，
   决定"能不能调这个方法、参数和返回值合不合法"。
3. **角色继承**：`RoleHierarchy`，让 `hasRole('ADMIN')` 自动拥有下级角色。
4. **数据级权限**：`PermissionEvaluator`，判断"能不能操作这条具体数据"。

### 4.2 对应代码

| 层 | 类/方法 | 位置 |
| --- | --- | --- |
| URL 规则 | `WebSecurityConfig` 各链的 `authorizeRequests` | `WebSecurityConfig.java:218-231` 等 |
| 方法安全开关 | `MethodSecurityConfig` | `config/MethodSecurityConfig.java` |
| 角色继承 | `MethodSecurityConfig.roleHierarchy` | `MethodSecurityConfig.java:37-42` |
| 表达式处理器 | `webExpressionHandler` / `createExpressionHandler` | `MethodSecurityConfig.java:47-62` |
| 方法注解示例 | `MethodDemoService` | `security/method/MethodDemoService.java` |
| 数据权限 | `DomainPermissionEvaluator` | `security/method/DomainPermissionEvaluator.java` |
| 服务层二次校验 | `UserAdminService` | `security/rbac/UserAdminService.java` |

### 4.3 代码在做什么

**(1) URL 规则：按 HTTP 方法 + 权限码细分**

```java
// WebSecurityConfig.java:222-231（节选）
.antMatchers(HttpMethod.POST, "/api/auth/login", ...).permitAll()  // 拿身份的地址放行
.antMatchers(HttpMethod.GET, "/api/users", "/api/users/**").hasAuthority("user:read")
.antMatchers(HttpMethod.POST, "/api/users", ...).hasAuthority("user:write")
.antMatchers("/api/audit/**").hasAuthority("audit:read")
.antMatchers("/api/method/**").authenticated()   // 只要求登录，细分交给方法注解
.anyRequest().authenticated();                    // 其余 /api/** 登录即可
```

注意 `permitAll` 的登录地址：此时调用方还没有令牌，真正的口令校验在 `AuthController` 里做。

**(2) 方法安全：为什么必须开 proxyTargetClass**

```java
// MethodSecurityConfig.java:23-25
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true,
                            jsr250Enabled = true, proxyTargetClass = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration { ... }
```

- `proxyTargetClass = true`：服务类没有接口，默认 JDK 代理要求有接口，不开这个开关注解不生效。
- 注解必须写在 **public 方法**上，且**从别的 Bean 调用**。同一个类内部自调用不经过代理，注解等于没写。
- 三种注解风格都开了：`prePostEnabled`（表达式）、`securedEnabled`（`@Secured`）、`jsr250Enabled`（`@RolesAllowed`）。

**(3) 角色继承：只对表达式生效，@Secured/@RolesAllowed 不吃**

```java
// MethodSecurityConfig.java:37-42
hierarchy.setHierarchy("ROLE_ADMIN > ROLE_AUDITOR\nROLE_ADMIN > ROLE_USER");
```

`hasRole('ADMIN')` 会同时拥有 AUDITOR 和 USER。但 `@Secured("ROLE_AUDITOR")` 和
`@RolesAllowed("ROLE_USER")` 要的是**精确角色名**，所以管理员调 `/api/method/jsr250-user` 会 403。
这是模块刻意保留的演示差别（`MethodDemoService.java:54-62`），测试
`SecurityFlowTest.methodSecurityHonorsExpressionsAndIgnoresHierarchyForJsr250` 专门验证了它。

**继承要配两次**，否则只有注解认、过滤链不认：

```java
// MethodSecurityConfig.java:47-62
@Bean DefaultWebSecurityExpressionHandler webExpressionHandler(...)  // URL 规则用
@Override MethodSecurityExpressionHandler createExpressionHandler()  // 方法注解用
```

**(4) 五种方法注解的语义**（`MethodDemoService.java`）

| 注解 | 时机 | 示例 | 含义 |
| --- | --- | --- | --- |
| `@PreAuthorize` | 方法前 | `hasRole('ADMIN')` | 有 ADMIN 角色才能进 |
| `@PreAuthorize` | 方法前 | `hasPermission(#userId,'user','read')` | 走数据权限计算器，`#userId` 是参数 |
| `@PostAuthorize` | 方法后 | `returnObject == authentication.name or hasRole('ADMIN')` | 返回值是本人或管理员才放行 |
| `@PreFilter` | 方法前 | `filterObject == authentication.name or hasRole('ADMIN')` | 先过滤参数列表，去掉别人的名字 |
| `@Secured` / `@RolesAllowed` | 方法前 | `ROLE_AUDITOR` / `ROLE_USER` | 精确角色，不吃继承 |

**(5) 数据级权限：本人放行 + 未知类型默认拒绝**

```java
// DomainPermissionEvaluator.java:25-33
if (!"user".equals(targetType) || ...) return false;   // 未知资源类型一律拒绝
if (isSelf(authentication, targetId)) return true;      // 本人看自己的资料放行
return hasAuthority(authentication, targetType + ":" + permission);  // 否则要 user:read
```

`hasPermission` 只在方法注解里走这里，URL 上的 `hasAuthority` 不经过它。
"未知类型默认拒绝"避免新写的资源类型因为没人处理而被默认允许。

**(6) URL 和方法各写一次，防进程内绕过**

```java
// UserAdminService.java:55-57
@PreAuthorize("hasAuthority('user:read')")
@Transactional(readOnly = true)
public List<UserView> list() { ... }
```

URL 规则拦的是 HTTP 请求；如果以后有别的服务在**进程内**直接调 `list()`，URL 规则拦不住它，
方法注解是第二道闸。控制器保持很薄（`UserController.java`），不写权限 if，
让"谁能调用"集中在链和注解两处。

### 4.4 现实项目中的用法

- 粗粒度（能不能进模块）用 URL 规则，细粒度（能不能操作这条数据）用方法注解 + PermissionEvaluator。
- 角色继承适合"管理员天然拥有下级权限"，但要注意 `@Secured` 不吃继承，混用时统一用表达式。
- 数据权限的"本人放行"是常见需求（看自己的资料不需要 user:read），用 `isSelf` 实现。
- 方法注解失效的三大原因：没开 `proxyTargetClass`、写在 private/自调用方法上、表达式处理器没配继承。

---

## 5. 攻击防护：限流、锁定、CSRF、安全响应头

### 5.1 知识点：登录是攻击重灾区

登录接口面临三类攻击：**暴力破解**（猜口令）、**撞库**（批量试）、**账号探测**（试用户名）。
防护手段按位置分：

- **认证之前**：按 IP 限流（挡批量）、维护开关（挡维护期）。
- **认证之中**：失败计数 + 锁定（挡暴力）、隐藏用户不存在（挡探测）。
- **传输之上**：CSRF（挡跨站伪造）、安全响应头（挡 XSS/点击劫持/缓存泄露）。

### 5.2 对应代码

| 防护 | 类 | 位置 |
| --- | --- | --- |
| 登录前置过滤器 | `LoginProtectionFilter` | `security/auth/LoginProtectionFilter.java` |
| 维护开关 | `MaintenanceLoginGuard` | `security/auth/MaintenanceLoginGuard.java` |
| 滑动窗口限流 | `SlidingWindowLimiter` | `security/auth/SlidingWindowLimiter.java` |
| 失败锁定 | `LoginAttemptService` | `security/auth/LoginAttemptService.java` |
| 审计 + 锁定联动 | `AuthenticationAuditListener` | `security/auth/AuthenticationAuditListener.java` |
| IP 解析 | `ClientIpResolver` | `common/web/ClientIpResolver.java` |
| CSRF | `sessionChain` 的 `CookieCsrfTokenRepository` | `WebSecurityConfig.java:289` |
| 安全响应头 | `WebSecurityConfig.applyHeaders` | `WebSecurityConfig.java:356-364` |

### 5.3 代码在做什么

**(1) 登录防护放在安全链之前，且不读请求体**

```java
// WebSecurityConfig.java:178-185
registration.addUrlPatterns("/api/auth/login", "/api/auth/password-expired",
                            "/api/auth/ldap", "/session/login");
registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
```

`LoginProtectionFilter` 是 Servlet 过滤器，比 Security 链更早，只挂这四个登录地址。
**不读取 body**，这样后面的控制器或表单过滤器还能再把用户名口令读出来。
调用链：请求先到这里（维护开关、按 IP 限流）→ 再进入对应的 SecurityFilterChain。

**(2) 滑动窗口限流：键是"IP + 路径"**

```java
// SlidingWindowLimiter.java:17-31
Deque<Long> deque = windows.computeIfAbsent(key, ...);
synchronized (deque) {
    while (!deque.isEmpty() && deque.peekFirst() < now - window) deque.pollFirst(); // 清过期
    if (deque.size() >= limit) return false;   // 一分钟内已达上限
    deque.addLast(now);
    return true;
}
```

- 键是 `IP + 路径`（`LoginProtectionFilter.java:53`），所以打爆登录接口不会把别的接口一起限掉。
- 计数只在本进程内存，**多实例会各自计数**，部署多实例要换共享存储，否则限流被摊薄。
- `ClientIpResolver`（`common/web/ClientIpResolver.java`）默认只用 TCP 连接地址，
  **不信任 `X-Forwarded-For`**——那是调用方自己填的，信任它换一个头就能换一个计数桶，限流被绕开。
  只有确实经过自己控制的反向代理时才把 `trust-forwarded` 设 true。

**(3) 失败锁定：只在"用户名存在且口令错"时加一**

```java
// LoginAttemptService.java:26-37
users.findByUsername(username).ifPresent(user -> {
    int fails = user.getFailedAttempts() + 1;
    if (fails >= maxFailures) {
        user.setAccountNonLocked(false);
        user.setLockUntil(now + lockMinutes);
    }
});
```

- 用户不存在时什么都不做，避免用这个接口给不存在的用户名制造记录。
- 锁定期间再失败**不会把锁定期往后推**（`onBadCredentials` 只加次数，不重置 `lockUntil`）。
- 解锁在 `SecurityUserDetailsService.unlockIfDue`，下次登录自然触发，无需定时任务。

**(4) 审计监听器：事件驱动，而非每个接口各写一遍**

```java
// AuthenticationAuditListener.java:42-68
@EventListener
public void onSuccess(AuthenticationSuccessEvent event) { attempts.onSuccess(...); save(...); }
@EventListener
public void onFailure(AbstractAuthenticationFailureEvent event) {
    if (event.getException() instanceof BadCredentialsException) attempts.onBadCredentials(...);
    save(...);
}
```

用户名口令登录、会话登录、HTTP Basic 都经过 `AuthenticationManager`，都会发认证事件，
所以**一个监听器覆盖所有登录方式**。监听器自己吞掉异常：审计库出问题不应把已成功的登录变成 500。
`AuthController` 的免登录改密口（`/api/auth/password-expired`）和 LDAP 口在口令错时
**手动补发** `AuthenticationFailureBadCredentialsEvent`，让它们也累计锁定次数。

**(5) CSRF：会话链开，API 链关**

```java
// WebSecurityConfig.java:289（会话链）
http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
```

- 会话登录靠 Cookie 识别身份，恶意网站可替用户发请求，**必须开 CSRF**。
- `withHttpOnlyFalse()` 让前端脚本能读到 `XSRF-TOKEN` Cookie，再放进请求头 `X-XSRF-TOKEN`。
- API 链用 Bearer（请求头），浏览器不会自动携带，CSRF 无意义，故 `csrf().disable()`。
- `GET /session/csrf`（`SessionController.java:40-47`）把令牌发给前端。

**(6) 安全响应头：每条链都写**

```java
// WebSecurityConfig.java:356-364
http.headers()
    .contentTypeOptions().and()        // 不让浏览器猜内容类型（防 MIME 嗅探）
    .frameOptions().deny()             // 不允许被嵌进别的网站 iframe（防点击劫持）
    .xssProtection().block(true).and()
    .cacheControl().and()              // 不缓存带身份的响应
    .referrerPolicy().policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN).and()
    .contentSecurityPolicy("default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
```

CSP 设成 `default-src 'none'`，因为这些接口返回 JSON，不需要加载脚本或样式。

### 5.4 现实项目中的用法

- 限流 + 锁定是登录防护的标配，但限流键要选对（IP+路径），且多实例必须共享计数。
- 不要信任 `X-Forwarded-For` 做安全决策，除非前面是自己控制的代理。
- 审计用事件监听统一收口，比在每个登录接口里写一遍更不易漏。
- CSRF 只对有 Cookie 会话的链开；纯令牌 API 关掉，否则前端每次都要带令牌很麻烦。

---

## 6. 会话管理：会话固定、并发控制、注册表

### 6.1 知识点：Cookie 会话的三个风险

- **会话固定**：攻击者事先塞给用户一个自己知道的会话号，用户登录后攻击者用同一会话号冒充。
  对策：登录成功后**换新会话号**。
- **并发会话**：同一账号多处登录。对策：限制最大会话数，超限踢掉最早的或拒绝新登录。
- **会话泄露**：会话号被脚本偷走。对策：Cookie 设 `HttpOnly` + `SameSite`。

### 6.2 对应代码

| 功能 | 类/配置 | 位置 |
| --- | --- | --- |
| 会话登录策略 | `LoginSessionStrategy` | `security/session/LoginSessionStrategy.java` |
| 会话注册表 | `WebSecurityConfig.sessionRegistry` | `WebSecurityConfig.java:112-115` |
| 会话事件发布 | `httpSessionEventPublisher` | `WebSecurityConfig.java:121-124` |
| 并发/固定配置 | `sessionChain` 的 `sessionManagement` | `WebSecurityConfig.java:294-306` |
| 会话查询 | `SessionController` | `web/SessionController.java` |
| Cookie 属性 | `application.yml` 的 `server.servlet.session.cookie` | `application.yml:37-43` |

### 6.3 代码在做什么

**(1) 自定义表单过滤器不会自动套用 sessionManagement，所以单独写策略**

```java
// LoginSessionStrategy.java:30-39
if (request.getSession(false) == null) request.getSession(true);  // changeSessionId 要求会话已存在
concurrent.onAuthentication(...);   // 先处理旧会话：超限就让最早的失效
request.changeSessionId();          // 再换新会话号（防会话固定）
register.onAuthentication(...);     // 最后把新会话号登记进 SessionRegistry
```

**顺序不能反**：换号要放在登记之前，否则注册表里留下的是已作废的旧会话号。
`ConcurrentSessionControlAuthenticationStrategy.setExceptionIfMaximumExceeded(false)`
表示超限时**踢掉最早的会话**让这次登录成功，而不是拒绝新登录。

**(2) 注册表 + 事件发布器配套**

```java
// WebSecurityConfig.java:112-124
@Bean SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }  // 内存在线会话表
@Bean HttpSessionEventPublisher httpSessionEventPublisher() { ... }            // 会话销毁事件转给 Security
```

没有 `HttpSessionEventPublisher`，用户关浏览器或会话超时后，注册表不知道该删这条会话。
注册表是内存的，进程重启清空，多机要换共享存储。

**(3) 会话链的完整配置**

```java
// WebSecurityConfig.java:294-306（节选）
.sessionCreationPolicy(IF_REQUIRED)          // 只有登录成功才建会话
.invalidSessionStrategy(...401 SESSION_EXPIRED...)
.sessionFixation().changeSessionId()         // 登录后换号
.maximumSessions(properties.getSession().getMaximum())  // 默认 1
.maxSessionsPreventsLogin(false)             // 超限踢最早，不拒新
.sessionRegistry(sessionRegistry)
.expiredSessionStrategy(...401 "会话已在其他地方登录"...)
```

**(4) Cookie 属性**（`application.yml:37-43`）

```yaml
cookie:
  http-only: true     # 脚本读不到会话 Cookie，减轻 XSS 偷会话
  same-site: lax      # 跨站请求不带这个 Cookie；CSRF 另有一套，不靠放开 SameSite 凑
```

**(5) 在线会话查询只返回会话号前 8 位**

```java
// SessionController.java:62-69
String prefix = sessionId.substring(0, 8);   // 不把完整会话号交给浏览器
```

`GET /session/active` 在链上要求 `ROLE_ADMIN`，且只返回前缀，避免完整会话号泄露。

### 6.4 现实项目中的用法

- 会话固定防护（`changeSessionId`）是登录成功的必做项，Spring Security 默认就开，自定义过滤器要记得手动调。
- "同一账号只留最新登录"用 `maximumSessions(1) + maxSessionsPreventsLogin(false)`；
  "拒绝新登录"则设 true。
- 多机部署时 `SessionRegistryImpl` 要换成基于 Redis/数据库的实现，否则踢人只在本机生效。

---

## 7. 密码管理：BCrypt、策略、历史、过期

### 7.1 知识点：口令只存摘要，不存明文

- **BCrypt**：自带随机盐，同样口令每次摘要不同，所以比较必须用 `matches`，不能字符串相等。
  计算成本（strength）越大越慢，撞库也更贵。
- **密码策略**：长度、字符类别、禁含用户名、禁用弱口令。
- **历史口令**：最近 N 次旧摘要不能再用。
- **密码过期**：到期仍能登录，但业务接口被拦，只能改密。

### 7.2 对应代码

| 功能 | 类/方法 | 位置 |
| --- | --- | --- |
| 编码器 | `WebSecurityConfig.passwordEncoder` | `WebSecurityConfig.java:78-81` |
| 复杂度策略 | `PasswordPolicy` | `security/password/PasswordPolicy.java` |
| 改密/历史/过期 | `PasswordService` | `security/password/PasswordService.java` |
| 过期拦截 | `PasswordExpiryFilter` | `security/jwt/PasswordExpiryFilter.java` |
| 改密接口 | `PasswordController` | `web/PasswordController.java` |
| 免登录改密 | `AuthController.changeExpired` | `web/AuthController.java:120-133` |

### 7.3 代码在做什么

**(1) BCrypt 成本 10**

```java
// WebSecurityConfig.java:78-81
return new BCryptPasswordEncoder(10);
```

10 是计算成本，越大越慢。口令只存摘要，比较用 `matches`。

**(2) 策略：上限 72 是因为 BCrypt 只取前 72 字符**

```java
// PasswordPolicy.java:16-17
public static final int MIN_LENGTH = 8;
public static final int MAX_LENGTH = 72;   // BCrypt 只取前 72 字符，更长等于没保护
```

校验大小写、数字、符号各至少一个，禁含用户名（挡 `alice`/`Alice@123` 这种易猜组合），
命中禁用名单（`application.yml` 的 `denylist`）也拒绝。

**(3) 历史比对用 matches，不能字符串相等**

```java
// PasswordService.java:83-88
for (PasswordHistory history : recent) {
    if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
        throw ...("新密码不能与最近使用过的密码相同");
    }
}
```

BCrypt 每次摘要不同，所以历史表存的是"被换下来的旧摘要"，比对必须用 `matches`。
改密成功后把旧摘要存进历史表，并 `trim` 到 `history-size`（默认 5）条。

**(4) 密码过期：仍能登录，但只能改密**

```java
// PasswordExpiryFilter.java:27-32 放行名单
"/api/auth/me", "/api/auth/logout", "/api/password/change", "/api/password/policy"
```

`SecurityUser.isCredentialsNonExpired()` 固定 true（见第 2 节），所以过期用户能登录、
能拿令牌，但除放行名单外的业务接口都被这个过滤器挡下（40310）。
`isExpired`（`PasswordService.java:42-48`）按 `passwordChangedAt + max-age-days` 判断，
`max-age-days=0` 表示不检查。

**(5) 改密后作废旧令牌、签发新的一对**

```java
// PasswordController.java:55-64
passwordService.changeOwn(...);
tokens.revokeAccess(authorization);       // 旧访问令牌进黑名单
tokens.revokeAllRefresh(current.getId()); // 该用户所有刷新令牌作废
return tokens.issue(fresh);               // 发新的一对，前端要换
```

**(6) 免登录改密口也累计锁定**

`POST /api/auth/password-expired`（`AuthController.java:120-133`）允许密码过期、
还没令牌时用旧口令换新口令。旧口令错时补发失败事件累计锁定，避免这个口子被无限试口令。
用户名不存在和旧口令错返回同一句话，防账号探测。

### 7.4 现实项目中的用法

- BCrypt 是 Spring Security 默认推荐；要更强可换 Argon2/scrypt，但历史比对逻辑不变（都用 matches）。
- 密码过期"仍能登录但只能改密"比"直接拒绝登录"体验好，用户不会被困在门外。
- 改密后必须作废所有旧令牌，否则旧令牌在过期前仍可用，等于改密没生效。

---

## 8. 协议集成：Basic / OAuth2 / LDAP / SAML2

### 8.1 知识点：一种登录方式 = 一条链 + 一个目录项

模块把每种登录方式抽象成 `SecurityProtocol`（仅用于目录展示），真正生效还要在配置里
加一条匹配该路径的 `SecurityFilterChain`。**只加目录项不会打开登录入口**。

### 8.2 对应代码

| 协议 | 链/入口 | 目录项 | 开关 |
| --- | --- | --- | --- |
| 用户名口令→JWT | `apiChain` + `AuthController.login` | `FormPasswordProtocol` | 常开 |
| JWT Bearer | `JwtAuthenticationFilter` | `JwtBearerProtocol` | 常开 |
| HTTP Basic | `basicChain` | `HttpBasicProtocol` | 常开 |
| Cookie 会话 | `sessionChain` | `SessionCookieProtocol` | 常开 |
| OAuth2/OIDC | `oauth2Chain`（条件） | `OAuth2LoginProtocol` | `security.oauth2.enabled` |
| LDAP 绑定 | `LdapBindClient` + `/api/auth/ldap` | `LdapBindProtocol` | `security.ldap.enabled` |
| SAML2 | 未接入（占位） | `Saml2Protocol` | 固定 false |

目录项都在 `security/protocol/`，由 `SecurityCatalog` 收进 `GET /api/security/modules`。

### 8.3 代码在做什么

**(1) HTTP Basic：控制器里没有认证代码**

```java
// WebSecurityConfig.java:256
http.httpBasic().authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, true));
```

`BasicAuthenticationFilter` 取出 `Authorization: Basic base64(用户名:口令)` → 交给
`AuthenticationManager` 校验 → 通过后进入 `BasicDemoController`。因为 STATELESS，
每次请求都重新认证。`basic=true` 的 EntryPoint 会多写 `WWW-Authenticate` 头，
告诉客户端"这里要用 Basic，不是跳登录页"。

**(2) OAuth2：条件装配 + 外部身份映射本地账号**

```java
// OAuth2ClientSecurityConfig.java:28-41
@ConditionalOnProperty(prefix = "security.oauth2", name = "enabled", havingValue = "true")
public class OAuth2ClientSecurityConfig {
    @Bean @Order(0)   // 比 /api/** 更靠前，回调地址不会被 JWT 链抢走
    public SecurityFilterChain oauth2Chain(...) {
        http.requestMatchers().antMatchers("/oauth2/**", "/login/oauth2/**");
        http.oauth2Login().successHandler(oauth2SuccessHandler);
    }
}
```

成功处理器（`OAuth2ClientSecurityConfig.java:49-60`）按外部返回的用户名查本地账号，
**本地有这个人就签发 JWT，没有就返回 40106**。角色仍以本地库为准，不直接采用外部系统给的角色。
默认关闭，避免没配客户端时启动失败。

**(3) LDAP：只回答"目录口令对不对"，不挂全局 Manager**

```java
// LdapBindClient.java:21-29
BindAuthenticator authenticator = new BindAuthenticator(contextSource);
authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});
this.provider = new LdapAuthenticationProvider(authenticator);
```

`@ConditionalOnProperty(security.ldap.enabled)` 控制是否创建。绑定通过后仍用同名本地用户签发 JWT。
**不注册成全局 Provider**，避免普通登录失败后又去目录试一遍（见第 2 节）。
内嵌目录用 `--spring.profiles.active=ldap` 启动（`application-ldap.yml`），仅本机演示。

**(4) SAML2：占位，未接入**

`Saml2Protocol.enabled()` 固定 false，工程没有 SAML 依赖。以后要接时再引依赖、
配身份提供方、单独加一条链；只改这个类不会打开登录。

### 8.4 现实项目中的用法

- 多协议系统按"一协议一链"组织，目录项只用于展示/自检，不参与放行。
- 外部身份（OAuth2/LDAP）登录后**映射到本地账号**再授权，角色以本地为准，避免外部角色直接透传。
- 条件装配（`@ConditionalOnProperty`）让可选协议默认关闭，不配也能启动。

---

## 8.5 短信验证码登录（阿里云 + 自动注册）

### 8.5.1 知识点：换一种凭证，而非新建过滤链

短信登录本质是把"用户名+口令"换成"手机号+验证码"，仍是一次同步身份校验。
它**不需要新过滤链**，和 LDAP 一样是挂在 `apiChain`（`/api/**`）上的新端点 + 新服务，
复用 `JwtTokenService.issue`、`SecurityUserDetailsService`、`LoginProtectionFilter`。

实现采用**路线 B（控制器编排）**：不挂全局 `AuthenticationManager`，由控制器显式调用服务，
与模块现有 LDAP/OAuth2 风格一致。失败时手动补发认证失败事件，复用审计与锁定。

### 8.5.2 对应代码

| 组件 | 类 | 位置 |
| --- | --- | --- |
| 验证码状态机 | `SmsCodeService` | `security/sms/SmsCodeService.java` |
| 发送扩展点 | `SmsSender` | `security/sms/SmsSender.java` |
| 演示发送（打印） | `LogSmsSender` | `security/sms/LogSmsSender.java` |
| 阿里云发送 | `AliyunSmsSender` | `security/sms/AliyunSmsSender.java` |
| 登录编排+自动注册 | `SmsLoginService` | `security/sms/SmsLoginService.java` |
| 入口 | `SmsLoginController` | `web/SmsLoginController.java` |
| 目录项 | `SmsLoginProtocol` | `security/protocol/SmsLoginProtocol.java` |
| 手机号字段 | `SecUser.phone` / `SecUserRepository.findByPhone` | `domain/` |

### 8.5.3 代码在做什么

**(1) 验证码状态机**（`SmsCodeService`）：`SecureRandom` 生成 6 位码，内存存储（多实例需换 Redis）。
四道防护：发送间隔限流（默认 60 秒，防轰炸）、试错上限（默认 5 次，超限作废）、
成功立即作废（防重放）、TTL（默认 300 秒）。

**(2) 发送通道可替换**：`SmsSender` 接口 + `@ConditionalOnProperty(security.sms.provider)`。
`log`（默认）只打印验证码供本机演示；`aliyun` 用官方 SDK `com.aliyun:dysmsapi20170525:4.6.0`
调 `SendSms`，AccessKey 从环境变量 `ALIBABA_CLOUD_ACCESS_KEY_ID/SECRET` 读取，不硬编码。

**(3) 自动注册**（`SmsLoginService.register`）：未注册手机号自动建账号，用户名即手机号，
挂默认 `ROLE_USER`，密码为随机不可登录值（短信登录不靠密码）。

**(4) 调用链**：

```
POST /api/auth/sms/send
  LoginProtectionFilter（IP 限流）→ 按手机号限流 → 生成码 → SmsSender.send
POST /api/auth/sms/login
  LoginProtectionFilter → apiChain(permitAll)
  → SmsCodeService.verify → findByPhone（无则自动注册）
  → 检查账号状态 → SecurityUserDetailsService 加载 → JwtTokenService.issue
```

### 8.5.4 现实项目中的用法

- 验证码必须放共享存储（Redis）才能多实例部署；本模块单实例用内存。
- 生产切 `provider=aliyun`，配好签名/模板，AccessKey 走环境变量或密钥管理。
- 演示模式回显验证码仅限本机，生产严禁回显或打印验证码。

---

## 8.6 扫码登录（跨设备授权 + 轮询）

### 8.6.1 知识点：不是校验凭证，而是异步状态机

扫码登录是**跨设备授权**：PC 展示二维码、已登录的手机扫码确认、PC 轮询拿令牌。
概念上等同 OAuth 2.0 设备授权流程。它没有"一次性 authenticate(token)"的时刻，
所以**不做成 AuthenticationProvider**，而是控制器编排 + 状态存储。

扫码**不认证手机**，而是把手机已有的 JWT 身份**委托**给 PC：scan/confirm 都由已登录手机调用。

### 8.6.2 对应代码

| 组件 | 类 | 位置 |
| --- | --- | --- |
| 状态机 | `QrLoginService` | `security/qr/QrLoginService.java` |
| 入口 | `QrLoginController` | `web/QrLoginController.java` |
| 请求体 | `QrRequest` | `web/dto/QrRequest.java` |
| 目录项 | `QrLoginProtocol` | `security/protocol/QrLoginProtocol.java` |

### 8.6.3 代码在做什么

**(1) 状态机**：`WAITING → SCANNED → CONFIRMED →（PC 取走）EXPIRED`，另有 `CANCELED`。
qrId 用 UUID 防猜测，短 TTL（默认 120 秒）。

**(2) 鉴权差异**：`create`/`status` 是 permitAll（qrId 即凭证）；`scan`/`confirm`/`cancel`
不在放行名单，落到 `apiChain` 末尾的 `anyRequest().authenticated()`，由 JWT 过滤器校验手机令牌。

**(3) 一次性取令牌**：只有 PC 轮询 `/status` 才消费令牌（取走即转 EXPIRED，防重放）；
手机 scan/confirm 回显用 `peek`（不消费），避免提前把 PC 还没取的令牌消费掉。

**(4) 防替他人确认**：`confirm` 校验二维码绑定的就是当前手机用户。

**(5) 调用链**：

```
PC: GET /qr/create → 拿 qrId 渲染二维码 → 轮询 GET /qr/status
手机: POST /qr/scan(带手机JWT) → 置 SCANNED 绑定用户
      POST /qr/confirm → 校验绑定 → 为该用户 issue JWT 暂存 → 置 CONFIRMED
PC: 轮询 status → CONFIRMED + 取走 JWT → qrId 作废
```

### 8.6.4 现实项目中的用法

- 单实例用内存 Map；多实例必须换 Redis（PC 轮询和手机确认可能打到不同机器）。
- 轮询最省事、零新增依赖；要实时可换 WebSocket/SSE（需另加依赖）。
- 令牌一次性取走 + 短 TTL + 绑定校验，是扫码安全的三个关键点。

---

## 9. 可扩展架构：三个扩展点与运行时目录

### 9.1 知识点：开闭原则的三个落地

模块预留三个扩展点，新增功能时**实现接口 + 注册成 Bean**即可，不必改核心代码：

| 扩展点 | 接口 | 作用 | 现有实现 |
| --- | --- | --- | --- |
| 登录前守卫 | `LoginGuard` | 登录前总开关（维护、内网限制） | `MaintenanceLoginGuard` |
| 令牌声明定制 | `TokenCustomizer` | 签发前往 JWT 加声明 | `PasswordExpiredClaimCustomizer` |
| 协议目录 | `SecurityProtocol` | 目录接口多一项 | 7 个协议实现 |

### 9.2 对应代码

- `security/auth/LoginGuard.java`、`MaintenanceLoginGuard.java`
- `security/jwt/TokenCustomizer.java`、`PasswordExpiredClaimCustomizer.java`
- `security/protocol/SecurityProtocol.java` 及 7 个实现
- `security/extension/SecurityCatalog.java`、`web/SecurityCatalogController.java`

### 9.3 代码在做什么

**(1) LoginGuard：返回原因即拒绝**

```java
// LoginGuard.java
Optional<String> reject(HttpServletRequest request);
```

`LoginProtectionFilter` 在读请求体之前依次询问每个实现，返回原因就 503。
要加"只允许内网"这类开关，实现本接口即可，不必改过滤器。

**(2) TokenCustomizer：签名前被调用**

`JwtTokenService.issue` 注入 `List<TokenCustomizer>`，签名前逐个调用。
声明只是提示，授权仍以数据库为准——改了这个声明并不能绕过 `PasswordExpiryFilter`。

**(3) SecurityCatalog：把容器里的扩展收成清单**

```java
// SecurityCatalog.java:25-29
public SecurityCatalog(List<SecurityProtocol> protocols, List<LoginGuard> guards,
                       List<TokenCustomizer> customizers) { ... }
```

Spring 自动把同类型的所有 Bean 注入成 List，所以新增实现会自动出现在
`GET /api/security/modules` 里。这个接口**不做授权判断**，只列清单方便对照源码。

### 9.4 现实项目中的用法

- 用"接口 + List 注入"做插件式扩展，是 Spring 里实现开闭原则的标准手法。
- 运行时目录（self-describing）便于运维自检"当前装了哪些协议/守卫/扩展"。
- 注意目录项≠生效：协议要真正可用还得加对应的过滤链。

---

## 10. 统一异常与响应：401 / 403 / 业务码

### 10.1 知识点：401 与 403 的语义

- **401 Unauthorized**：你是谁还不知道（未登录/令牌失效）。
- **403 Forbidden**：我知道你是谁，但这件事不让你做（权限不足）。

过滤器里的 401/403 由 EntryPoint/DeniedHandler 直接写响应，**到不了** `@RestControllerAdvice`；
控制器里抛的异常才由 `GlobalExceptionHandler` 接住。

### 10.2 对应代码

| 出口 | 类 | 位置 |
| --- | --- | --- |
| 未登录 401 | `RestAuthenticationEntryPoint` | `security/auth/RestAuthenticationEntryPoint.java` |
| 权限不足 403 | `RestAccessDeniedHandler` | `security/auth/RestAccessDeniedHandler.java` |
| 控制器异常 | `GlobalExceptionHandler` | `common/exception/GlobalExceptionHandler.java` |
| 统一响应体 | `ApiResult` / `ResultCode` | `common/api/` |

### 10.3 代码在做什么

**(1) EntryPoint：不跳 HTML 登录页，写 JSON 401**

```java
// RestAuthenticationEntryPoint.java:30-36
if (basic) response.setHeader("WWW-Authenticate", "Basic realm=\"allst\"");
JsonResponses.write(response, mapper, 401, ApiResult.fail(UNAUTHORIZED, "未登录或登录已失效"));
```

调用方是接口而非浏览器页面，所以不跳转。Basic 链多写 `WWW-Authenticate`。

**(2) GlobalExceptionHandler：把认证异常映射成业务码**

```java
// GlobalExceptionHandler.java:53-65
if (ex instanceof LockedException)   → 401 LOCKED "账号已锁定"
if (ex instanceof DisabledException) → 401 DISABLED "账号已停用"
if (ex instanceof AccountExpiredException) → 401 DISABLED "账号已过期"
其余 → 401 BAD_CREDENTIALS "用户名或密码错误"
```

不写这个方法，`AuthController.login` 里 `authenticate` 抛的异常会变成 500，
调用方无法区分"密码错了"和"服务器坏了"。用户不存在和口令错统一文案，防探测。

**(3) 登录失败处理器区分锁定/停用**

`JsonLoginFailureHandler`（会话链）对 `LockedException`/`DisabledException` 给不同业务码，
其余都说"用户名或密码错误"，不区分用户不存在。

### 10.4 现实项目中的用法

- 前后端分离项目用 JSON 出入口替换默认的 HTML 登录页/错误页。
- 认证异常要在 `@RestControllerAdvice` 里显式接住，否则变 500。
- 对外文案统一"用户名或密码错误"，内部日志才区分具体原因。

---

## 11. 关键设计取舍与常见疑问

### 11.1 为什么访问令牌不落库，刷新令牌却落库？

访问令牌每次请求都用，落库会让无状态接口退化成"每次查库"，失去 JWT 的意义；
签名+过期已足够，主动作废用黑名单补。刷新令牌只在换证时用，频率低，落库才能做轮换和撤销。

### 11.2 为什么 JWT 里写了角色，过滤器还要再查库？

令牌里的角色只是给前端展示。再查库保证**停用账号、改角色后下一次请求就生效**，
不必等令牌过期。这是"无状态"与"实时撤销"之间的折中。

### 11.3 为什么密码过期还能登录？

`isCredentialsNonExpired()` 固定 true，让用户能登录、能调改密接口。
若返回 false，用户直接登录失败，连改密都进不去，会被困在门外。
限制由 `PasswordExpiryFilter` 做：除改密相关地址外都拦。

### 11.4 为什么角色继承对 @Secured 不生效？

`RoleHierarchy` 只作用于表达式（`hasRole`/`hasAuthority`）。`@Secured`/`@RolesAllowed`
要精确角色名，不走表达式处理器。模块刻意保留这个差别作演示，业务里要管理员也能进就用表达式。

### 11.5 为什么限流默认不信任 X-Forwarded-For？

那是调用方自己填的头，信任它换一个头就能换一个计数桶，限流被绕开。
只有确实经过自己控制的反向代理时才设 `trust-forwarded=true`。

### 11.6 为什么方法注解要配两次表达式处理器？

URL 规则用 `DefaultWebSecurityExpressionHandler`，方法注解用 `DefaultMethodSecurityExpressionHandler`，
两者各配一次角色继承和 PermissionEvaluator，否则只有注解认继承、过滤链不认（或反之）。

### 11.7 多实例部署要注意什么？

- `SlidingWindowLimiter`、`SessionRegistryImpl` 是内存的，多实例要换共享存储（Redis/数据库）。
- 注销名单和刷新表用 JPA 数据库，天然共享，无需改。
- JWT 密钥所有实例必须一致。

### 11.8 测试覆盖了什么？

`SecurityFlowTest`（集成测试）验证：JWT 登录→调保护接口、授权区分审计员/普通用户、
刷新轮换与退出注销、方法注解吃继承而 JSR250 不吃、会话/Basic/公开 hello。
`SecurityUnitTest`（单元测试）验证密码策略与滑动窗口限流。
运行：`mvn test`（默认 H2，无需外部数据库）。

---

## 附录 A：功能 → 代码文件速查表

| 功能 | 主要文件 | 关键方法/行 |
| --- | --- | --- |
| 四条过滤链 | `config/WebSecurityConfig.java` | `apiChain`/`basicChain`/`sessionChain`/`publicChain` |
| 认证入口 | `config/WebSecurityConfig.java` | `authenticationManager` (103) |
| 口令校验 | `config/WebSecurityConfig.java` | `daoAuthenticationProvider` (90) |
| 账号加载 | `security/user/SecurityUserDetailsService.java` | `loadUserByUsername` (31) |
| 权限装配 | `security/user/SecurityUsers.java` | `authorities` (23) |
| JWT 签发/校验 | `security/jwt/JwtTokenService.java` | `issue`/`parse`/`refresh` |
| JWT 过滤器 | `security/jwt/JwtAuthenticationFilter.java` | `doFilterInternal` (42) |
| 密码过期拦截 | `security/jwt/PasswordExpiryFilter.java` | `doFilterInternal` (41) |
| URL 授权 | `config/WebSecurityConfig.java` | `authorizeRequests` (218) |
| 方法安全 | `config/MethodSecurityConfig.java` | 类注解 (24) |
| 角色继承 | `config/MethodSecurityConfig.java` | `roleHierarchy` (37) |
| 方法注解示例 | `security/method/MethodDemoService.java` | 各注解方法 |
| 数据权限 | `security/method/DomainPermissionEvaluator.java` | `hasPermission` (25) |
| 登录限流 | `security/auth/LoginProtectionFilter.java` | `doFilterInternal` (39) |
| 滑动窗口 | `security/auth/SlidingWindowLimiter.java` | `allow` (17) |
| 失败锁定 | `security/auth/LoginAttemptService.java` | `onBadCredentials` (27) |
| 审计监听 | `security/auth/AuthenticationAuditListener.java` | `onSuccess`/`onFailure` |
| 会话策略 | `security/session/LoginSessionStrategy.java` | `onAuthentication` (31) |
| 密码策略 | `security/password/PasswordPolicy.java` | `check` (22) |
| 改密/历史 | `security/password/PasswordService.java` | `replace` (78) |
| Basic 链 | `config/WebSecurityConfig.java` | `basicChain` (245) |
| OAuth2 链 | `config/OAuth2ClientSecurityConfig.java` | `oauth2Chain` (32) |
| LDAP 绑定 | `config/LdapBindClient.java` | `bind` (27) |
| 扩展点目录 | `security/extension/SecurityCatalog.java` | `describe` (31) |
| 401 出口 | `security/auth/RestAuthenticationEntryPoint.java` | `commence` (30) |
| 403 出口 | `security/auth/RestAccessDeniedHandler.java` | `handle` (27) |
| 异常映射 | `common/exception/GlobalExceptionHandler.java` | `authentication` (53) |
| 种子数据 | `bootstrap/SecurityDataInitializer.java` | `run` (43) |
| 令牌清理 | `job/TokenCleanupJob.java` | `clean` (28) |

> 行号以当前源码为准，改动后请以类名/方法名回溯。

## 附录 B：一次完整请求的调用链

### B.1 登录 `POST /api/auth/login`

```
LoginProtectionFilter（维护开关 → 按 IP 限流）
  → apiChain（该地址 permitAll，此时无令牌）
  → AuthController.login
  → AuthenticationManager.authenticate
  → DaoAuthenticationProvider
  → SecurityUserDetailsService.loadUserByUsername（查账号 + BCrypt 比对）
  → AuthenticationAuditListener（写审计 + 累计失败/重置）
  → JwtTokenService.issue（签访问令牌 + 刷新令牌）
  → 返回 TokenView
```

### B.2 业务 `GET /api/users`（带 Bearer）

```
apiChain
  → JwtAuthenticationFilter（验签 → 查黑名单 → 读库 → 放 SecurityContext）
  → PasswordExpiryFilter（密码过期则拦）
  → FilterSecurityInterceptor（hasAuthority("user:read")）
  → UserController.list
  → UserAdminService.list（@PreAuthorize 再校验一次）
  → 返回 JSON
```

### B.3 会话登录 `POST /session/login`

```
LoginProtectionFilter
  → sessionChain
  → CSRF 校验
  → UsernamePasswordAuthenticationFilter（表单 username/password）
  → AuthenticationManager 校验
  → LoginSessionStrategy（踢旧会话 → 换会话号 → 登记注册表）
  → JsonLoginSuccessHandler（返回当前用户 JSON）
```

### B.4 刷新 `POST /api/auth/refresh`

```
apiChain（permitAll）
  → AuthController.refresh
  → JwtTokenService.refresh（验签 → 作废旧 jti → 读账号状态 → 签新的一对）
  → 返回新 TokenView
```

---

*本文基于模块当前源码撰写。代码改动后，请以文内类名/方法名回溯，行号仅供参考。*