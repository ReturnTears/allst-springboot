# 08 限流

代码：`ratelimit/RateLimitService`、`ratelimit/RateLimitInterceptor`、注解 `common/RateLimit`。脚本在 `resources/lua/`。

限流必须在 Redis 里一次做完。先 GET 再在应用里判断，并发下会超卖额度。三种算法都是 Lua。

## 固定窗口

`INCR`，计数从 0 变 1 时设置过期。窗口从第一请求开始算。实现短，但窗口交界可能连续打进两个窗口的额度，瞬时流量接近限额的两倍。

## 滑动窗口

用 ZSET 记下每次请求的时间。先删掉窗口外的成员，再看数量。比固定窗口平滑，成员多时比计数器费空间。

## 令牌桶

Hash 里存剩余令牌和上次补充时间。按每秒速率把令牌加回去，不超过容量，然后消耗 1 个。允许短突发，之后被速率拦住。Redis 从 Lua 拿回去的小数会被截断，所以剩余令牌以字符串返回。

## 注解

`@RateLimit(limit = 5, windowSeconds = 10)` 走固定窗口，键是客户端 IP 加请求路径。拦截器返回 429。多实例部署时计数在 Redis 里，各节点共享限额。

网关或 Nginx 上的限流仍然需要。这里防的是进到应用之后的接口。

## 接口

- `GET /api/ratelimit/fixed?name=demo&limit=5&window=10`
- `GET /api/ratelimit/slide?name=demo&limit=5&window=10`
- `GET /api/ratelimit/token?name=demo&capacity=5&rate=1`
- `GET /api/ratelimit/protected`

## 验证

连续点击同一个按钮。第 6 次固定窗口或滑动窗口的 `allowed` 为 false。令牌桶一开始能通过 5 次，之后大约每秒恢复 1 次。注解接口第 6 次返回 429。
