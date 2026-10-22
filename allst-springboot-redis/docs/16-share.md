# 16 数据共享

代码：`share/ShareService`、`share/ShareController`。

微服务不适合互相读对方的数据库。变化不频繁、大家又都要看的数据，可以放在 Redis：功能开关、字典版本、实例是否还活着。

配置是 Hash `share:config`。每次修改都 `INCR share:config:ver`。调用方记住上次看到的版本，版本变了再刷新本地副本。不要每次请求都把整份配置读出来再比较。

实例心跳是字符串 `share:svc:{service}:{instance}`，TTL 30 秒。进程停掉后续期，键过期，列表里就不再出现。这比「下线时主动删除」可靠，因为进程可能来不及主动删除。

列举在线实例用 `SCAN`，匹配 `share:svc:*`。不用 `KEYS`，`KEYS` 会在键多的时候堵住 Redis。

共享缓存不是消息队列，也不是分布式事务。写入方要接受读者在一个很短的窗口里看到旧值。

## 接口

- `POST /api/share/config`，体：`{"field":"feature.rank","value":"on"}`
- `GET /api/share/config`
- `POST /api/share/heartbeat`，体：`{"service":"order","instance":"order-1"}`
- `GET /api/share/services`

## 验证

写两次配置，版本号应递增。发送心跳后能在在线实例里看到 `order-1`，TTL 接近 30。30 秒内不再心跳，它会从列表消失。
