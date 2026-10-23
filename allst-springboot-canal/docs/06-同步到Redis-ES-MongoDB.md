# 06 同步到 Redis、Elasticsearch、MongoDB

三个消费者收到的是同一份 `DataChangeEvent`。落库规则集中在 `ChangeDocument` 和 `BinlogCursor`。

## 公共约定

INSERT、UPDATE 写入 after 镜像的全部列，并附带这些保留字段：

| 字段 | 含义 |
| --- | --- |
| `_binlogFile` | 例如 `mysql-bin.000001` |
| `_binlogOffset` | 该文件内的偏移 |
| `_eventType` | INSERT 或 UPDATE |
| `_schema` | 库名 |
| `_table` | 表名 |

业务表不要使用这些列名。DELETE 不写文档，按主键删除。

目标名称：

| 下游 | 位置 | `demo.t_product` 的例子 |
| --- | --- | --- |
| Redis | `{prefix}:{schema}:{table}:{主键}` | `canal:demo:t_product:1001` |
| Elasticsearch | 索引 `{schema}_{table}` 全小写，文档 id 为主键 | 索引 `demo_t_product`，id `1001` |
| MongoDB | 库 `canal_sync`，集合名与 ES 索引相同，`_id` 为主键 | 集合 `demo_t_product` |

前缀来自 `sync.redis.key-prefix`，默认 `canal`。

ES 写入使用 `RefreshPolicy.IMMEDIATE`，联调时马上能查到。数据量大时应改回默认刷新周期，避免每次写入都刷新索引。

## Redis

`RedisSyncConsumer` 监听 `canal.sync.redis`，调用 `RedisSyncService`。

值全部是字符串，用 Hash 而不是一个 JSON 字符串，方便只看某一列。`HGETALL canal:demo:t_product:1001` 就是整行。

## Elasticsearch

客户端是 Boot 2.6 自带的 `RestHighLevelClient`，地址 `spring.elasticsearch.rest.uris`。

`EsSyncService` 用 Index API 按主键覆盖文档，用 Delete API 删除。索引不存在时，ES 7 默认会在第一次写入时自动创建。动态映射会把数字字符串继续当成字符串，因为 Canal 给出的列值本来就是字符串。搜索数字范围需要另做映射，本模块没有加。

## MongoDB

连接串 `spring.data.mongodb.uri`，默认库 `canal_sync`。`MongoSyncService` 用 `MongoTemplate.save`，`_id` 设成主键，所以重复 INSERT 是覆盖而不是再插一条。

## 查询接口

`SyncInspectController` 同时读三个下游，用来对照，不参与同步本身。

```text
GET /api/sync?schema=demo&table=t_product&id=1001
```

某一端连不上时，这一端返回 `error`，另外两端仍然返回。`schema`、`table`、`id` 只允许字母、数字、下划线、点、冒号和中划线。

## 旧位点

写入前先读目标里的 `_binlogFile` 和 `_binlogOffset`。规则在 `BinlogCursor`：

- 目标没有位点：写入。
- 新文件名更大：写入。MySQL 默认文件名零填充，字符串比较和序号一致。
- 同一文件且新偏移大于或等于已存偏移：写入。相等表示同一条消息的重试。
- 新位点更小：跳过，并打 info 日志。

三个下游各自保存位点，所以 ES 已经写到偏移 200 时，迟到的 100 不会把文档盖回去。Redis 若还停在 100，仍会接受 200。
