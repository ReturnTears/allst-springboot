# Canal 同步 MySQL 到 Redis、Elasticsearch、MongoDB

Java 11、Spring Boot 2.6.5。Canal 订阅 MySQL binlog，本应用把行变更发到 RabbitMQ，再由三条队列分别写入 Redis、Elasticsearch 和 MongoDB。

兄弟模块 `allst-springboot-mysql2es` 用的是 `mysql-binlog-connector-java` 直连 binlog。本模块改用 [Alibaba Canal](https://github.com/alibaba/canal)，并把下游从“写死一个存储”拆成消息队列上的多个消费者。

## 数据怎么走

```text
MySQL binlog
    -> Canal Server（解析 binlog，TCP 端口 11111）
    -> 本应用 CanalClientRunner（解析行变更，成功入队后才 ack）
    -> RabbitMQ exchange canal.sync.exchange
         -> canal.sync.redis   -> Redis Hash
         -> canal.sync.es      -> Elasticsearch 文档
         -> canal.sync.mongo   -> MongoDB 文档
```

一条 MySQL 变更会复制到三个队列。某个下游失败只进入自己的死信队列，不会挡住另外两个，也不会把 Canal 位点卡死。

## 文档

实现过程按这个顺序记在 `docs/`，排查时按编号回溯：

| 文档 | 内容 |
| --- | --- |
| [01-目标与架构](docs/01-目标与架构.md) | 要解决的问题和模块边界 |
| [02-环境准备](docs/02-环境准备.md) | 中间件、端口、启动命令 |
| [03-MySQL-Binlog](docs/03-MySQL-Binlog.md) | ROW 格式、复制账号、示例表 |
| [04-Canal订阅与解析](docs/04-Canal订阅与解析.md) | Server 配置和客户端 ack |
| [05-MQ解耦](docs/05-MQ解耦.md) | 交换机、队列、死信、重试 |
| [06-同步到Redis-ES-MongoDB](docs/06-同步到Redis-ES-MongoDB.md) | 三个下游怎么落库 |
| [07-幂等失败与回溯](docs/07-幂等失败与回溯.md) | 重复消息、旧消息、死信 |
| [08-联调验证](docs/08-联调验证.md) | 从插入一行到三个库都能查到 |

## 最快跑起来

在本模块目录：

```bash
docker compose -f deploy/docker-compose.yml up -d
mvn spring-boot:run
```

本机没有 `mvn` 时，用模块里的包装器：`.\mvnw.cmd spring-boot:run`。

等 Canal 和本应用都连上之后，再执行 `deploy/mysql/verify.sql`。然后访问：

```text
http://localhost:1238/api/sync/ping
http://localhost:1238/api/sync?schema=demo&table=t_product&id=1001
```

应用端口是 `1238`。MySQL 初始化脚本只建表和 Canal 账号，验证数据必须在订阅开始之后写入，原因写在 [03-MySQL-Binlog](docs/03-MySQL-Binlog.md)。
