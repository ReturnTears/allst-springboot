# 00 数据结构

代码：`structure/StructureService`、`structure/StructureController`。

这一章只演示命令本身。运行示例会先删除 `demo:struct:*`，再把命令重放一遍，结果和 redis-cli 看到的一致。其它章节用自己的键，不会被这里清掉。

## String

适合缓存正文、计数、分布式锁的持有者。示例做了 `SET`、`APPEND`、`SETEX`、`INCR` / `INCRBY`。

计数用 `INCR`，不要先 GET 再在应用里加一，那样并发会丢。

## Hash

适合一个对象的多个字段，例如用户资料。`HGET` 可以只拿一个字段，`HINCRBY` 可以给单个字段计数。对象很大、又总是整份读写时，直接存一条 String JSON 更简单，本项目的商品缓存就是这样做的。

## List

适合队列、时间线、最近 N 条记录。示例用 `RPUSH` + `LPOP` 表示先进先出，用 `LTRIM` 把列表裁到固定长度。消息队列那一章会在这个基础上做成可靠消费。

## Set

自动去重。`SISMEMBER` 判断在不在，`SINTER` 做共同标签或共同好友，`SUNION` 做并集。点赞用户、关注关系都是 Set。

## ZSet

每个成员有一个分数，按分数排序。`ZINCRBY` 加分是原子的，`ZREVRANGE` 取前 N 名，`ZREVRANK` 查名次。排行榜、延迟任务、滑动窗口限流都用它，只是分数的含义不同。

## Geo

地理位置命令的底层还是 ZSet，分数是经纬度编出来的 geohash。`GEOADD` 写入，`GEODIST` 算距离，`GEORADIUS` 查附近。业务上的门店查询在第 13 章，这里只看命令。

## HyperLogLog

`PFADD` / `PFCOUNT` 估算基数。写入 1000 次、只有 700 个不同用户时，计数接近 700，标准误差大约 0.81%。内存占用几乎不随用户数增长，适合 UV，不适合要精确到个位的金额。

## Bitmap

`SETBIT` 把某一位标成 1，`GETBIT` 读取，`BITCOUNT` 统计 1 的个数。签到、当天是否访问过，用「用户 + 日期」做偏移即可。位图很省空间，但偏移要连续，不要把用户 id 直接当偏移，否则会撑出一块巨大的稀疏位图。

## 接口

- `GET /api/structure/{type}/demo`
- `POST /api/structure/{type}/play`

`type` 可以是 `string`、`hash`、`list`、`set`、`zset`、`geo`、`hyperloglog`、`bitmap`。

`play` 的请求体至少要有 `op`。常用组合：

| 结构 | op | 其它字段 |
| --- | --- | --- |
| string | get / set / incr | key、value、ttl、delta |
| hash | get / set | key、field、value |
| list | range / push / pop | key、value |
| set | members / add / inter | key、value，或 a、b |
| zset | top / incr | key、member、delta、limit |
| geo | add / nearby | key、member、lng、lat、radiusKm |
| hyperloglog | add / count | key、value |
| bitmap | set / stat | key、offset、value |

## 验证

打开控制台「00 数据结构」，选中一种结构后点「运行示例」。也可以：

```bash
curl http://localhost:1236/api/structure/string/demo
```
