# TCP 服务端

入口是 `NettyTcpServerLifecycle`。Spring 调用它的 `start()` 时才创建线程组并 `bind`。端口写 `0` 时由操作系统分配，测试就是这么做的，实际端口用 `localPort()` 取。

## 启动参数

`ServerBootstrap` 使用 `NioServerSocketChannel`。父 Channel 设置 `SO_BACKLOG` 和 `SO_REUSEADDR`。每条接受进来的连接设置：

- `TCP_NODELAY`：小报文不等待凑包
- `SO_KEEPALIVE`：操作系统层的保活，代替不了应用心跳
- 写水位 32KB / 64KB：超过高水位后 `channel.isWritable()` 为 false，HTTP 下发返回 429

## Pipeline

`WarehouseServerInitializer` 为每条连接按这个顺序组装：

1. `IdleStateHandler`，只监控读空闲，默认 90 秒
2. `WarehouseFrameDecoder`
3. `WarehouseMessageDecoder`
4. `WarehouseFrameEncoder`
5. `WarehouseServerHandler`，指定放在 `warehouse-biz` 这个 `DefaultEventExecutorGroup` 上

前四站在 `warehouse-worker` 上执行。业务 Handler 放进独立线程组后，登录、扫码、回执的处理不会占住读循环。当前这些操作只碰内存；以后如果在这里查库，也要继续留在业务线程或异步客户端里，不要阻塞 worker。

## 会话

`DeviceSessionRegistry` 用设备号做键。

- 新登录：写入 Channel 属性 `warehouseDeviceId`，推送 `ONLINE`
- 同一设备号从另一条连接再次登录：关掉旧 Channel。旧连接的 `channelInactive` 发现登记表里已经不是自己，不会再推一次 `OFFLINE`
- 同一条连接换成另一个设备号：先把旧设备号从登记表移除并推 `OFFLINE`
- 连接断开：只有当前登记的还是这条 Channel 时才删除，并推 `OFFLINE`

`SO_KEEPALIVE` 发现不了「进程还在但枪已经不再发业务」的情况。90 秒没有读到任何帧，`userEventTriggered` 收到 `READER_IDLE`，主动关闭。客户端心跳默认 15 秒，正常设备不会被误杀。心跳本身不推到看板，看板每 3 秒拉一次 `GET /api/devices` 刷新「最后上报」。

## 业务分支

`WarehouseServerHandler.channelRead0`：

- `LOGIN`：`bind`，回 `ACK`，推 `ONLINE`
- `HEARTBEAT`：刷新 `lastSeenAt`，回 `ACK`
- `SCAN`：追加到 `RecentEventLog`（最多 500 条，新的在前），推 `SCAN`，回 `ACK`
- `ACK`：若 `requestId` 能对上一条 `SENT` 指令，改成 `ACKED` 并推 `COMMAND_ACK`
- `LOGOUT`：回 `ACK`，写完成后再关闭
- `COMMAND`：设备发指令视为协议错误，回 `ERROR` 并关闭

`exceptionCaught` 打一条警告后关闭连接，避免一条坏帧把线程打崩。
