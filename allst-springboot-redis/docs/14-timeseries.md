# 14 时间序列

代码：`timeseries/TimeSeriesService`、`timeseries/TimeSeriesController`。

Redis 本身没有时间序列类型。RedisTimeSeries 模块可以，但很多环境没装。这里用普通结构实现，任何 Redis 都能跑：

- ZSET `ts:index:{metric}`：成员是唯一点 id，分数是毫秒时间戳
- Hash `ts:value:{metric}`：点 id 到数值

写入时用「时间戳 + 纳秒」做成员，同一毫秒的两个点不会互相覆盖。

查询用 `ZRANGEBYSCORE` 取出时间范围内的点，最多 300 个。`bucketSeconds` 大于 0 时，在应用里按桶求平均。降采样放在应用里，是因为桶的宽度会变，不值得为每种宽度再存一份。

`retain` 删除某个时间之前的点和对应 Hash 字段。生产上要有保留策略，否则监控指标会一直涨。

高频率、要降采样和压缩的监控，更适合时序数据库。Redis 适合最近一段时间的热数据。

## 接口

- `POST /api/timeseries/{metric}`，体：`{"value":0.35,"timestamp":0}`，timestamp 为 0 表示现在
- `GET /api/timeseries/{metric}?from=0&to=0&bucketSeconds=60`
- `POST /api/timeseries/{metric}/retain`，体：`{"before":1700000000000}`

## 验证

对 `cpu` 连续写几个值，原始查询能看到每个点。`bucketSeconds=60` 时，同一分钟内的点合成一个平均值，并带上 `count`。
