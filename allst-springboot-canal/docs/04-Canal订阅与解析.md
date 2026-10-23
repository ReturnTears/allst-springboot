# 04 Canal 订阅与解析

## Server

镜像 `canal/canal-server:v1.1.7`。官方默认 destination 叫 `example`，传输模式是 TCP，所以没有改 `canal.properties`。

实例配置挂到容器内这个路径：

```text
/home/admin/canal-server/conf/example/instance.properties
```

宿主机文件是 `deploy/canal/instance.properties`。关键项：

| 配置 | 值 | 说明 |
| --- | --- | --- |
| `canal.instance.mysql.slaveId` | 1234 | 必须和 MySQL `server-id=1` 不同 |
| `canal.instance.master.address` | mysql:3306 | compose 网络里的服务名 |
| `canal.instance.dbUsername` / `dbPassword` | canal / canal | 对应 init.sql |
| `canal.instance.filter.regex` | `demo\\..*` | 只把 demo 库交给这个实例 |
| `canal.instance.tsdb.enable` | false | 演示环境不启用表结构时序库，少一个 H2 依赖问题 |
| `canal.instance.gtidon` | false | 本例用 binlog 文件名 + 偏移，不用 GTID |

`journal.name` 和 `position` 留空。Canal 会从当前 master 位点开始，并把位点记在自己的 meta 里。容器重建且没有保留 Canal 数据目录时，位点会重新从“当前”开始，中间漏掉的变更不会自动补。

过滤表达式写在 properties 文件里要双反斜杠。Canal 读出来的正则是 `demo\..*`，表示 demo 库任意表。

## 客户端

`CanalClientRunner` 使用官方 `CanalConnectors.newSingleConnector`，对应文档 [ClientExample](https://github.com/alibaba/canal/wiki/ClientExample)。

连接参数在 `application.yml` 的 `canal.client`：

```yaml
canal:
  client:
    host: 127.0.0.1
    port: 11111
    destination: example
    subscribe: "demo\\..*"
    batch-size: 100
```

`destination` 必须等于 Server 的实例名 `example`。`subscribe` 是客户端的二次过滤，这里和 Server 保持一致。YAML 双引号里的 `\\` 同样是一个反斜杠。

`canal.client` 把 `canal.protocol` 标成了 optional，只引 client 时编译器看不到 `CanalEntry`。模块 `pom.xml` 因此同时声明了这两个依赖。协议对象只留在 `canal` 包里，出了解析器就变成 `DataChangeEvent`。

拉取循环：

1. `connect`、`subscribe`。
2. `rollback`，从上次没确认的批次接着读。进程重启不会跳过已拉走但没 ack 的消息。
3. `getWithoutAck`。超时 1 秒，方便停止线程。
4. `CanalEntryParser` 把 `Entry` 转成 `DataChangeEvent`。
5. `DataChangePublisher` 写入 RabbitMQ。
6. 整批都发出去之后 `ack`。任一条发布失败就 `rollback`，并重连。

空批次（`batchId < 0` 或没有 Entry）不 ack，和官方示例一致。事务开始、事务结束、DDL 在解析器里丢掉，批次里如果只剩这些，仍然 ack，避免卡在同一条事务标记上。

单条 Entry 解析失败会打错误日志并跳过该条，然后继续 ack。这样一条损坏记录不会把位点永久卡住。代价是这一条不会进入下游，日志里能按 `logfile` 和 `offset` 对上。

## 解析规则

`CanalEntryParser` 只保留 `ROWDATA` 里的 `INSERT`、`UPDATE`、`DELETE`。

| 事件 | 主键来自 | 下游写入的列 |
| --- | --- | --- |
| INSERT | after | after |
| UPDATE | after，没有则 before | after。依赖 `binlog_row_image=FULL` |
| DELETE | before | 不写列，按下主键删除 |

联合主键用 `:` 拼成一个 `primaryValue`，例如 `8:2`。SQL `NULL` 写成空字符串，三个下游因此不用各自处理 null。这是演示约定，空字符串和数据库 NULL 在下游不再可区分。

`messageId` 由库、表、主键、binlog 文件、偏移和事件类型组成，用来在日志和死信里定位，不拿它当下游文档 id。下游文档 id 是主键。
