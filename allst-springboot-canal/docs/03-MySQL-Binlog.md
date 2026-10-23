# 03 MySQL Binlog

Canal 读的是 binlog，不是业务表的触发器。格式不对时，Canal 能连上，但拿不到变更前后的整行。

## 必须打开的项

`deploy/docker-compose.yml` 里 MySQL 的启动参数：

| 参数 | 值 | 原因 |
| --- | --- | --- |
| `server-id` | 1 | 复制拓扑里的身份，不能和 Canal 的 `slaveId` 相同 |
| `log-bin` | mysql-bin | 打开 binlog |
| `binlog-format` | ROW | 记录每一行的列值。STATEMENT 只记 SQL，Canal 无法可靠还原行 |
| `binlog-row-image` | FULL | UPDATE 的后镜像带上全部列。如果是 MINIMAL，下游整文档覆盖会把没改的列写丢 |

账号在 `deploy/mysql/init.sql`：

```sql
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED WITH mysql_native_password BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
```

MySQL 8 默认的 `caching_sha2_password` 和 Canal 1.1.7 的握手容易失败，所以指定 `mysql_native_password`。镜像也加了 `--default-authentication-plugin=mysql_native_password`。

权限只给复制和查询。Canal 不需要业务库的写权限。

## 示例表

`demo.t_product`，主键 `id`。下游用主键当 Redis field 集合的 key、ES 文档 `_id`、MongoDB `_id`。没有主键的表，`CanalEntryParser` 会打警告并跳过。

`updated_at` 使用 `ON UPDATE CURRENT_TIMESTAMP`，UPDATE 时 MySQL 会改这一列，binlog 里能看到它。

## 为什么 init.sql 里不插入数据

Canal 默认从连接成功时的 master 位点往后读，不会回放建库脚本里的 INSERT。那些语句发生在 Canal 启动之前。

验证数据放在 `deploy/mysql/verify.sql`，等 Canal Server 和本应用都已经在跑再执行。步骤在 [08-联调验证](08-联调验证.md)。

## 自己确认 binlog

```bash
docker exec -it allst-canal-mysql mysql -uroot -proot -e "SHOW VARIABLES LIKE 'log_bin'; SHOW VARIABLES LIKE 'binlog_format'; SHOW VARIABLES LIKE 'binlog_row_image'; SHOW MASTER STATUS;"
```

期望：`log_bin=ON`，`binlog_format=ROW`，`binlog_row_image=FULL`，并且能看到 `mysql-bin.00000x`。
