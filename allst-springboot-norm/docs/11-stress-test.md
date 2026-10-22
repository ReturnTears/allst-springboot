# 11. 压测

压测用来看健康检查在并发下的错误率、QPS 和延迟分位，不把签名、落库和限流算进基线。

## 入口

`com.allst.springboot.stress.ApiStressMain` 使用 JDK 自带的 `HttpClient`。它不是 Spring Bean，不会随应用启动，单元测试也不会调用它。

先启动应用，再执行：

```bash
mvn -DskipTests compile exec:java -Dexec.mainClass=com.allst.springboot.stress.ApiStressMain -Dexec.args="http://127.0.0.1:1234/api/health 20 200"
```

三个参数是地址、线程数、总请求数。输出包括耗时、成功数、错误数、QPS、p50、p95、p99 和最大值。

默认地址是 `http://127.0.0.1:1234/api/health`。这个接口不签名，访问日志不落库。

## 压测时建议

- 把 `com.allst.springboot.web.RequestAuditFilter` 的日志级别调到 WARN，避免打印日志成为瓶颈。
- 先打健康检查，得到接口和机器的基线。再单独打带签名的接口，观察签名和数据库的开销。
- `GET /api/v1/demo/ping` 每分钟只放行 5 次，打它得到的是限流结果，不是容量。
- 本机数字只作为回归对比，不作为容量承诺。换机器、换数据库后要重跑。

需要更长、更稳定的压测时，可以用同一地址接 wrk 或 JMeter。应用侧要观察的是错误率、p99、线程池队列和数据库连接池，而不是只看平均耗时。
