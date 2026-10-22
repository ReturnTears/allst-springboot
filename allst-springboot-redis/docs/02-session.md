# 02 会话存储

代码：`session/SessionService`、`session/SessionController`。

登录成功后发一个 32 位 token。会话是 Hash：`session:{token}`，里面有用户、登录时间和最近访问时间。每次读取会话都把 TTL 重新设成 30 分钟，这是滑动过期。绝对过期适合「最多登录 8 小时」，滑动过期适合「一直在用就不要踢下线」。

购物车是另一个 Hash：`cart:{userId}`，TTL 7 天。退出登录只删除会话，不删除购物车。两边寿命不同，就不要塞进同一个键。

示例不校验密码。生产环境要先核对数据库里的口令或第三方身份，再签发 token，并且只走 HTTPS。

也可以用 Spring Session 把 `HttpSession` 整份放进 Redis。本项目手写 token，是为了把键结构露出来。

## 接口

请求头 `X-Token` 携带登录返回的 token。控制台会把它记在 localStorage。

- `POST /api/session/login`，体：`{"username":"ada"}`
- `GET /api/session/me`
- `POST /api/session/logout`
- `GET /api/session/cart`
- `POST /api/session/cart`，体：`{"skuId":"keyboard","qty":1}`，qty 为负表示减少
- `DELETE /api/session/cart/{skuId}`

## 验证

登录后查看「当前会话」，`ttlSeconds` 接近 1800。退出后再访问会提示登录失效。重新登录后购物车还在。
