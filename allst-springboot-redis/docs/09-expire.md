# 09 数据过期

代码：`expire/ExpireService`、`expire/KeyExpireListener`。

Redis 可以给键设置秒级或毫秒级过期，到期后删除。适合验证码、优惠券预占、在线状态、临时页面。过期是惰性加定期删除，不是准时到点的定时器，业务上要能接受少量延迟。

`TTL` 有三种结果：

- 正数：剩余秒数
- `-1`：键在，但没有过期时间
- `-2`：键不存在

`PERSIST` 去掉过期时间。不要用「再 SET 一次不带 TTL」来续期，那会把值也改掉；续期用 `EXPIRE`。

本示例只允许操作 `temp:` 前缀，避免从控制台误删业务键。

## 过期事件

Redis 默认不发过期通知。需要：

```bash
redis-cli config set notify-keyspace-events Ex
```

`docker-compose.yml` 里已经加上这个参数。监听频道是 `__keyevent@{db}__:expired`，消息体是键名，键本身已经没了。回调里只把键名追加到 `expire:log`，不要在回调里做慢操作，也不要在回调里再写一个会马上过期的键，以免打成环。

通知可能丢。它适合做缓存失效后的附带动作，不适合作为唯一的业务触发器。真正要准时执行的任务用第 15 章的延迟队列。

## 接口

- `POST /api/expire`，体：`{"key":"coupon","value":"once","ttlSeconds":15}`
- `GET /api/expire/{key}`
- `POST /api/expire/{key}/persist`
- `GET /api/expire/log`

## 验证

写入一个 15 秒的键，马上查看 TTL，应接近 15。等它过期后再查，TTL 为 -2。如果打开了过期通知，日志里会出现 `temp:coupon`。
