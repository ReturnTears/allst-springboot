# 4. 限流

防止单个来源把接口打满。应用内是滑动窗口，集群和全站限额交给网关。

## 实现

| 类 | 作用 |
| --- | --- |
| `@RateLimit(permits, periodSeconds)` | 标在接口方法上 |
| `RateLimitAspect` | 在幂等切面之外执行，被限流的请求不占用幂等键 |
| `SlidingWindowLimiter` | 按 key 记录窗口内的时间戳 |

key 是 `客户端 IP + 方法 + 路径`。窗口滑出的时间戳会丢掉，当前窗口内达到 `permits` 就拒绝。

示例接口：

```java
@GetMapping("/ping")
@RateLimit(permits = 5, periodSeconds = 60)
public Result<String> ping()
```

地址是 `GET /api/v1/demo/ping`。同一 IP 60 秒内第 6 次返回 HTTP 429，body.code 为 `42901`。

健康检查没有这个注解，压测打 `/api/health` 时测到的是接口本身，而不是限流器。

## 生产注意

`SlidingWindowLimiter` 的计数在单机内存里。多实例时每个节点各算各的，总限额会变成实例数乘以配置值。全站限流用网关或 Redis；这个注解适合保护特别贵的接口，作为进程内的最后一道限制。

阈值写在注解上，改阈值需要发版。需要动态调整时，把 permits 改成读配置。
