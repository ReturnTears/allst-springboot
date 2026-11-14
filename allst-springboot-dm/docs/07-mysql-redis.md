# MySQL 8 与 Redis 在示例里的落点

模块默认**不依赖任何外部服务就能跑测试**。运行时用 MySQL 8 落订单，Redis 只作为缓存装饰器和命令幂等的可选实现。

## MySQL 8

`src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/allst_dm?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
    username: root
    password: ""
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        jdbc.time_zone: Asia/Shanghai
```

要点：

- 用户 `root`、密码为空，写成 `password: ""`；库 `allst_dm` 由 `createDatabaseIfNotExist=true` 首次连接自动创建，不用手工建库。
- MySQL 8 默认 `caching_sha2_password`，URL 里保留 `allowPublicKeyRetrieval=true`；驱动用 `com.mysql.cj.jdbc.Driver`（版本由父 pom 管理的 8.0.19 提供）。
- `serverTimezone=Asia/Shanghai` + Hibernate `jdbc.time_zone`，避免 `Instant` 落库差 8 小时。
- 表由 JPA `ddl-auto: update` 生成，不需要导入 SQL 脚本。生产别用 `update`，用 Flyway/Liquibase 管迁移。
- `open-in-view: false`，避免视图渲染期间还开着 Session。

唯一实体 `domain.OrderRecord` → 表 `dm_order`：

| 列 | 说明 |
| --- | --- |
| `id` | 自增主键 |
| `order_no` | 订单号，唯一；来自抽象工厂的渠道号段 |
| `buyer` / `sku` / `quantity` | 基本信息 |
| `original_amount` / `discount` / `payable` | 原价、优惠、应付（跨境含税） |
| `status` | `OrderStatus` 枚举，字符串存储 |
| `pay_channel` / `request_id` | 支付渠道、幂等键 |
| `created_at` | 创建时间 |

`OrderRecordRepository` 是仓储：`findByOrderNo`、`findByRequestId`、`findAll(Pageable)`。外观、迭代器、命令落库都只依赖它，不写 SQL。

哪些模式真的碰了数据库：

- **外观**：`findByRequestId` 做请求级幂等。
- **命令**：`execute` 里 `repository.save`，`undo` 里 `delete`。
- **迭代器**：`PagedOrderCursor` 用 `findAll(PageRequest.of(...))` 分批拉。
- **仓储**本身：把持久化与领域逻辑分开（见 [06](06-spring-patterns.md)）。

事务边界在 `PlaceOrderFacade.place()` 上（`@Transactional`）：状态推进、命令落库、事件发布在同一事务；仓库通知挂在 `AFTER_COMMIT`，提交后才发。

## 测试用 H2

`application-test.yml`（放在 `src/main/resources`，与仓库其它模块一致）在 `@ActiveProfiles("test")` 时覆盖数据源：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:alldm;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
dm:
  redis:
    enabled: false
```

`mvn -pl allst-springboot-dm test` 因此不连 MySQL、不连 Redis，CI 上可直接跑。33 个测试共用一个 H2 内存库，造数据用 UUID 保证互不冲突。

## Redis（可选）

`support.KeyValueStore` 是一个很小的接口：`get` / `put` / `putIfAbsent` / `delete`。两个实现由开关切换：

| 实现 | 启用条件 | 用在哪 |
| --- | --- | --- |
| `InMemoryKeyValueStore` | `dm.redis.enabled=false`（默认，`matchIfMissing=true`） | 测试、无 Redis 环境 |
| `RedisKeyValueStore` | `dm.redis.enabled=true` | 真实运行，`StringRedisTemplate` |

用到的两个地方：

1. **装饰器缓存**（`CachedQuote`）：`dm:price:{sku}`，TTL 60 秒；`PriceQuoteService.evict(sku)` 调价时失效。
2. **命令幂等**（`CommandInvoker`）：`dm:cmd:{commandId}`，`putIfAbsent` 对应 Redis `SET key value EX 300 NX`，TTL 300 秒。多实例部署时同一个命令只会执行一次；彻底失败会 `delete` 释放键，让上游重试能真正重跑。

切到 Redis 的步骤：

```yaml
dm:
  redis:
    enabled: true
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

然后确保本机 6379 可连。`spring-boot-starter-data-redis` 已引入，Lettuce 是懒连接，所以默认配置下即使没有 Redis 也不会在启动时报错。

**为什么默认关闭**：这个模块的主要内容是设计模式，不是 Redis 运维；让示例代码在没有外部依赖的机器上也能 `mvn test` 跑通，比「真实感」更重要。需要演示分布式幂等时再打开开关。

**注意**：`KeyValueStore` 不在 JPA 事务里，命令幂等标记和订单落库不是同一个原子操作。生产里要用「数据库唯一索引 / 事务消息 / 本地消息表」来兜底，TTL 到期后的重复请求也要考虑。示例把它简化，是为了把命令模式的结构讲清楚。
