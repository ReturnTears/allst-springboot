# 05 实时分析

代码：`analytics/AnalyticsService`、`analytics/AnalyticsController`。

一次访问写四份数据，按天分键，避免一个键无限长大：

- `analytics:pv:{yyyyMMdd}`：`INCR`，精确的页面浏览量
- `analytics:uv:{yyyyMMdd}`：HyperLogLog，估算独立用户
- `analytics:hour:{yyyyMMdd}`：Hash，字段是小时，值是该小时的次数
- `analytics:hot:{yyyyMMdd}`：ZSet，成员是页面，分数是点击

同一天同一个用户多次访问，PV 增加，UV 几乎不变。UV 是估算，不要拿它做结算。

这些键没有设置过期。生产上应按天 `EXPIRE`，或者落到数据库后删除，否则按天的键会一直占着。

## 接口

- `POST /api/analytics/hit`，体：`{"userId":"ada","page":"home"}`
- `GET /api/analytics/summary?day=20261022`，不传 day 就是今天

## 验证

用同一个用户连续打两次，再换一个用户打一次。汇总里 PV 为 3，UV 接近 2，热门页里能看到 `home`。
