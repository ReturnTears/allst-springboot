# WebSocket 看板

看板不走 WebFlux 的 WebSocket，而是 `NettyWebSocketServerLifecycle` 在 9001 上单独 listen。这样看板上的慢消费者不会占用设备接入的 worker，HTTP 接口的线程也不会和握手编解码缠在一起。

## Pipeline

`DashboardWebSocketInitializer` 对每条连接：

1. `HttpServerCodec`
2. `HttpObjectAggregator`（64KB），把 HTTP 握手收成一个完整请求
3. `WebSocketServerProtocolHandler("/ws/dashboard")`，处理升级、Ping/Pong 和关闭帧
4. 共享的 `DashboardWebSocketHandler`

`channelActive` 发生在握手完成之前，那时还不能发文本帧。Handler 等 `userEventTriggered` 里出现 `HandshakeComplete`，再登记 Channel 并推一份快照。

快照类型是 `SNAPSHOT`，payload 里有当前设备列表和最近 50 条扫码。它只写给这一条新连接，不进 SSE，避免「又有一个浏览器打开了」变成所有订阅者的噪音。

之后的增量由 `RealtimePushService.broadcast` 写 `TextWebSocketFrame`。`Channel.writeAndFlush` 可以从业务线程调用，Netty 会把写操作排到这条连接自己的 EventLoop。每个 Channel 单独 new 一个帧，因为写出后 ByteBuf 会被释放，不能多条连接共用同一个帧对象。

登记表是 `CopyOnWriteArrayList`。连接关闭时 `closeFuture` 和 `channelInactive` 都会移除，重复移除没有副作用。看板没有读空闲：浏览器可以一直不发消息，服务端单方面推。

## 消息

| type | 何时出现 | payload |
| --- | --- | --- |
| `SNAPSHOT` | 握手刚完成 | `devices`、`events` |
| `ONLINE` | 设备登录 | `DeviceView` |
| `OFFLINE` | 当前会话的连接断开 | 设备号 |
| `SCAN` | 扫码入库 | `ScanEvent` |
| `COMMAND` | HTTP 已把指令写入 Channel | `CommandRecord`，状态 `SENT` |
| `COMMAND_ACK` | 设备回了 ACK | `CommandRecord`，状态 `ACKED` |
| `COMMAND_FAILED` | 写入 Channel 失败 | `CommandRecord`，状态 `FAILED` |

## 页面

`src/main/resources/static/index.html` 是仓内接入台，没有构建步骤。

- 打开后先拉 `/api/events` 和 `/api/devices`，再读 `/api/meta`，用返回的端口连接 WebSocket。页面和接口在 8080，套接字在 9001。
- 流水按 `type + requestId` 去重，所以接口拉到的历史和 WebSocket 再推来的同一条扫码只显示一次。
- 设备列表每 3 秒再拉一次，用来刷新心跳时间。上下线同时靠 WebSocket 立刻改状态。
- 套接字断开后 3 秒重连。
- 条码和设备号用 `textContent` 写入 DOM，不拼 HTML。

页面上的「下发动作」和「记一笔扫码」分别调用指令接口和模拟扫码接口。失败时把错误体里的 `message` 显示在底部。
