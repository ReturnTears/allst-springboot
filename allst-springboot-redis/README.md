# Redis 开发规范示例

这个模块把日常开发里会碰到的 Redis 用法收在一处：8 种数据结构，再加上缓存、会话、排行榜、队列、实时分析、分布式锁、发布订阅、限流、过期、全页缓存、社交关系、推荐、地理位置、时间序列、延迟任务、服务间共享和持久化。

每一项都有可调用的接口、Vue3 控制台和一份独立说明，方便以后回头看当时为什么这样写。

## 技术选择

- Spring Boot 2.6、Java 11
- Spring Data Redis（Lettuce）负责键值、Stream、GEO、脚本
- Redisson 3.23 负责分布式锁和布隆过滤器
- MyBatis 访问 MySQL。没有 MySQL 时可以用内存库 H2
- 控制台是 Vue3，静态资源由本服务直接提供，不需要单独构建前端

键名集中在 `RedisKeys`。业务值存成普通字符串或 JSON，用 `redis-cli` 可以直接看。

## 启动

先准备 Redis。需要看「数据过期」的事件日志时，还要打开键空间通知：

```bash
docker compose up -d
```

等价的手工配置：

```bash
redis-cli config set notify-keyspace-events Ex
```

默认连接本机 MySQL，库名 `allst_redis`，账号 `root` / `root`。连接串带了 `createDatabaseIfNotExist=true`，MySQL 已启动时会自己建库。

```bash
mvnw spring-boot:run
```

Windows PowerShell 用 `.\mvnw.cmd spring-boot:run`。没有 MySQL 时：

```bash
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

打开 [http://localhost:1236/](http://localhost:1236/)。端口沿用原来的 `1236`。

可用环境变量覆盖连接：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`MYSQL_URL`、`MYSQL_USER`、`MYSQL_PASSWORD`。

Redis 没启动时，应用仍能起来，控制台会显示 Redis DOWN，调用接口会返回 503。Redisson 使用了延迟初始化，避免在启动阶段因为连不上 Redis 而退出。

## 文档

| 编号 | 主题 | 说明 |
| --- | --- | --- |
| 00 | [数据结构](docs/00-structure.md) | String、Hash、List、Set、ZSet、Geo、HyperLogLog、Bitmap |
| 01 | [缓存](docs/01-cache.md) | Cache-Aside、穿透、击穿、雪崩 |
| 02 | [会话存储](docs/02-session.md) | 登录态与购物车 |
| 03 | [排行榜和计数器](docs/03-rank.md) | ZSET、浏览量、点赞去重 |
| 04 | [消息队列](docs/04-mq.md) | List 可靠队列与 Stream |
| 05 | [实时分析](docs/05-analytics.md) | PV、UV、小时桶、热门页 |
| 06 | [分布式锁](docs/06-lock.md) | SET NX 与 Redisson |
| 07 | [发布订阅](docs/07-pubsub.md) | 广播、补看、SSE |
| 08 | [限流](docs/08-ratelimit.md) | 固定窗口、滑动窗口、令牌桶 |
| 09 | [数据过期](docs/09-expire.md) | TTL 与过期事件 |
| 10 | [全页缓存](docs/10-page.md) | 渲染结果缓存 |
| 11 | [社交功能](docs/11-social.md) | 关注、时间线、在线状态 |
| 12 | [实时推荐](docs/12-recommend.md) | 兴趣分与共现 |
| 13 | [地理位置](docs/13-geo.md) | 附近门店 |
| 14 | [时间序列](docs/14-timeseries.md) | 不依赖 RedisTimeSeries 模块 |
| 15 | [任务调度](docs/15-schedule.md) | 延迟队列 |
| 16 | [数据共享](docs/16-share.md) | 配置与心跳 |
| 17 | [持久化](docs/17-durable.md) | 查看 RDB / AOF |

代码包和文档一一对应，例如缓存在 `com.allst.springboot.redis.cache`，说明在 `docs/01-cache.md`。

## 示例数据

应用第一次启动时，如果商品表和页面表是空的，会写入 6 个商品和页面 `home`。之后重启不会覆盖你改过的库存和正文。布隆过滤器会按当前商品重建。

排行榜、社交关系、门店、推荐行为由控制台上的「填充」按钮写入，避免每次启动冲掉手工数据。
