# 07 发布订阅

代码：`pubsub/NotifyHub`、`pubsub/PubSubController`、`config/RedisListenerConfig`。

`PUBLISH` 只把消息交给当时在线的订阅者，不保存，不保证送达。适合在线广播：库存变更、简单通知。订单、支付这类不能丢的消息用第 04 章的队列或专门的消息中间件。

允许的频道只有三个：`notify:broadcast`、`notify:order`、`notify:im`。

`NotifyHub` 订阅这三个频道。收到消息后做两件事：

- 写进 `pubsub:recent`，只留最近 50 条，给刚打开页面的人补看
- 推给当前进程上的 SSE 连接

SSE 是服务端到浏览器的单向通道。发布接口仍然是普通 POST。多个应用节点时，每个节点都订阅 Redis，本节点的浏览器连接由本节点推送。

## 接口

- `POST /api/pubsub/publish`，体：`{"channel":"broadcast","message":"维护通知"}`
- `GET /api/pubsub/recent`
- `GET /api/pubsub/stream`，`text/event-stream`

## 验证

打开控制台「07 发布订阅」，保持页面不关，点发布。实时日志区应出现同一条消息。刷新后再看「最近消息」，还能看到它。
