# 17 持久化

代码：`durable/PersistenceService`、`durable/PersistenceController`。

Redis 的工作区在内存里。进程退出后，能不能恢复取决于持久化。

## RDB

RDB 是某一时刻的快照。`save` 规则决定多久做一次，例如 3600 秒内至少 1 次写入就保存。恢复快，但两次快照之间的写入会丢。

`BGSAVE` 在子进程里做，不挡住主线程太久。最近一次是否成功看 `rdb_last_bgsave_status`，时间看 `rdb_last_save_time`。

## AOF

AOF 把写命令追加到文件。`appendonly yes` 时，重启会重放命令。丢的数据比 RDB 少，文件更大，重启回放更慢。`aof_last_write_status` 不是 `ok` 时要处理磁盘问题。

两者可以同时开：用 AOF 恢复得更全，用 RDB 做备份和迁移。`docker-compose.yml` 里的 Redis 打开了 AOF。

只开内存、关掉两种持久化，适合纯缓存。缓存丢了可以从数据库再加载。会话、排行榜、延迟任务如果也在里面，就要接受重启后丢失，或者打开持久化。

## 这个接口做什么

`GET /api/durable` 只执行 `INFO` 和 `CONFIG GET`。它不修改 `appendonly`，也不主动 `BGSAVE`。改持久化策略要在 redis.conf 或运维流程里做，不应该从一个演示接口触发。

返回里每一行都有字段名、当前值和这句话在说什么。

## 验证

```bash
curl http://localhost:1236/api/durable
```

使用本仓库的 compose 启动时，`aof_enabled` 应为 1，`appendonly` 为 yes。如果 Redis 是默认安装、没有开 AOF，这里会如实显示 no，缓存以外的数据在重启后不会保留。
