# 运行、接口与验收

## 启动

```text
mvn -pl allst-springboot-dm spring-boot:run
```

- Java 11 语法编译（本机 JDK 17 也能跑，pom 里用 `<release>11</release>`），Spring Boot 2.6.5。
- 端口 `8367`。
- 需要本机 MySQL 8：`root` / 空密码，库 `allst_dm` 自动创建，表由 JPA 生成。
- Redis 默认关闭；打开方式见 [07](07-mysql-redis.md)。

模块目录下也可以用 `mvnw.cmd spring-boot:run`、`mvnw.cmd test`。

测试不连任何外部服务：

```text
mvn -pl allst-springboot-dm test
```

当前结果：`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`，覆盖 23 种模式的关键行为、端到端下单、幂等、校验失败、事务后事件和 HTTP 接口。

## 接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| POST | `/api/dm/orders` | 走完整条下单主线，返回 `trace` |
| GET | `/api/dm/orders/{orderNo}` | 按订单号查库 |
| GET | `/api/dm/orders?batchSize=2` | 迭代器分批扫描全部订单 |
| GET | `/api/dm/patterns` | 26 条场景速查表（与 [02](02-scenario-map.md) 同源） |
| GET | `/api/dm/demo` | 列出可单独运行的 23 个模式 key |
| GET | `/api/dm/demo/{pattern}` | 单独运行一个模式并返回可观察结果 |
| GET | `/api/dm/overview` | 校验链、策略、渠道、状态表等子系统清单 |
| GET | `/api/dm/stats` | 单例规则、享元池、装饰链命中、AOP 计数、事件、命令、快照 |
| GET | `/hello/{name}` | 脚手架自带探针 |

响应统一是 `Result<T>`：`{ code, message, data, timestamp }`。业务规则拒绝与参数校验失败都收敛成 `code=400`（`GlobalExceptionHandler`）。

## curl 演练

下单（把 `requestId` 换掉可以再来一次；用同一个则命中幂等）：

```bash
curl -X POST http://localhost:8367/api/dm/orders \
  -H "Content-Type: application/json" \
  -d '{"buyer":"tester","sku":"SKU-PHONE","unitPrice":199.00,"quantity":2,
       "vip":true,"payChannel":"ALIPAY","channel":"DOMESTIC",
       "requestId":"demo-001","riskScore":10}'
```

```bash
curl http://localhost:8367/api/dm/patterns
curl http://localhost:8367/api/dm/demo/strategy
curl http://localhost:8367/api/dm/demo/observer
curl http://localhost:8367/api/dm/orders?batchSize=2
curl http://localhost:8367/api/dm/stats
```

23 个 demo key：`singleton`、`factory-method`、`abstract-factory`、`builder`、`prototype`、`adapter`、`bridge`、`composite`、`decorator`、`facade`、`flyweight`、`proxy`、`chain`、`command`、`interpreter`、`iterator`、`mediator`、`memento`、`observer`、`state`、`strategy`、`template`、`visitor`。

## 一次下单返回的 trace 长什么样

`POST /api/dm/orders` 的 `data` 里有业务字段（`orderNo`、`status`、`payable`、`strategy`、`waybillNo`……）和一条 `trace`，大致是：

```text
责任链: 通过 5 关
组合: AND(金额>=100, OR(VIP, 新人), NOT(跨境单), 风控通过) => true
解释器(SpEL): amount >= 100 and (vip or newUser) and riskScore < 80 => true
策略: 命中 VIP，优惠 59.70
建造者: AUTO-demo-001(下单自动命中规则,FULL_REDUCTION,priority=10)
抽象工厂: DM20261114000001 / 国内订单：开具增值税发票
享元: SKU-PHONE/AllstPhone/实物商品 ... 池大小 1
装饰器: 目录价 2999.00 / {dbReads=1, cacheHits=0, ...}
原型: 活动-DM20261114000001 白名单 3 个 SKU
原型(作用域): 购物车草稿 []
工厂方法: ALIPAY 流水 ALI...
状态: CREATED --PAY--> PAID
命令: OK，尝试 1 次
中介者: 4 个同事按序执行
观察者: 发布 OrderPlacedEvent（仓库通知在 AFTER_COMMIT）
备忘录: 保存快照 下单前购物车 ...
模板方法: [校验通过, 明细 3 行, 生成完成: DOMESTIC]
访问者: {recognisedAmount=3397, invoiceCategories=[...]}
适配器: SF 运单 SF...
桥接: SMS->tester:订单 ... 已支付
代理: [BEFORE available, AFTER available]
单例: 当前规则 [VIP_DISCOUNT, ...]
外观: 结束，事务提交后触发提交后事件
```

事务提交后，`/api/dm/stats` 的 `events.afterCommit` 里会多一条「提交后通知仓库」。

## 端到端验证点

`PlaceOrderFlowTest` 覆盖了四件事，改代码后重点看它：

1. 正常下单金额、状态、运单号正确，trace 覆盖三类模式；
2. 同一 `requestId` 重复提交返回 `duplicate=true`，库里只多一笔；
3. 风控分 95 被责任链拦下，不落库；
4. 事务内审计先执行、提交后仓库通知后执行；跨境渠道的合规说明正确。

## 常见启动问题

| 现象 | 原因 |
| --- | --- |
| 启动报 `Communications link failure` | 本机 MySQL 没起，或不是 `root`/空密码；检查 3306 |
| 报 `Unknown database` | `createDatabaseIfNotExist=true` 未生效（被自定义 URL 覆盖），手工建 `allst_dm` |
| 端口占用 | `server.port: 8367` 改成空闲端口 |
| `Access denied for user 'root'` | 密码不是空；改 `spring.datasource.password` |
| 测试想连了 MySQL | 测试必须带 `@ActiveProfiles("test")`；`application-test.yml` 在 `src/main/resources` |
| 打开 Redis 后接口报连接错 | `dm.redis.enabled=true` 但 6379 没起；测试环境把它设回 false |
| 时间戳差 8 小时 | 检查 `serverTimezone=Asia/Shanghai` 与 Hibernate `jdbc.time_zone` |
