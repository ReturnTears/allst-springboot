# TCP 客户端

一台枪对应一个 `WarehouseDeviceClient`。模拟器里的多台枪共用一个 `device-client` EventLoopGroup，这是 Netty 客户端的常规用法：线程数跟枪的数量无关。

## 拨号

每次连接都 new 一个 `Bootstrap` 和一个 `WarehouseClientInitializer`。`ChannelInitializer` 初始化完会从 Pipeline 里摘掉，复用同一个实例在部分 Netty 版本上会碰到「非 Sharable Handler 不能添加两次」。每次新建可以避开这个问题。

Pipeline 与服务端对称：帧解码、JSON 解码、编码、`WarehouseClientHandler`。客户端没有业务线程组，指令回执很短，直接在 I/O 线程里写 `ACK`。

连接成功后立刻发送 `LOGIN`，并在这条 Channel 的 EventLoop 上 `scheduleAtFixedRate` 心跳。心跳任务挂在这条连接的 `closeFuture` 上，连接一断就取消，不会误取消下一轮连接的心跳。

## 断线重连

`running` 为 true 时才重连。`connectInFlight` 保证同一时刻只有一次拨号：

- 连接失败：等待 `reconnect-seconds`（默认 5 秒）再试
- 旧连接的关闭回调晚到：如果新连接已经在线，或已经有一次拨号在进行，不再额外拨号
- `stop()` 先把 `running` 设为 false，再关 Channel。关闭回调里看到标志位，不会再重连

`ownsGroup` 只在调用方独占线程组时为 true。模拟器自己持有线程组，每台枪传 false，由 `DeviceSimulator.stop()` 统一关闭。

## 收到指令

`WarehouseClientHandler` 只对 `COMMAND` 做动作：按原 `requestId` 回 `ACK`，再通知可选的监听器。测试用这个监听器确认枪真的看到了 `MOVE`。其他帧（例如服务端对扫码的 `ACK`）只在 debug 级别记录。

## 模拟器

`DeviceSimulator` 的 phase 是 100，晚于 TCP 服务的 phase 0，所以端口先开，枪再来连。见 [生命周期](08-生命周期.md)。

设备号格式是 `WH-SCANNER-%02d`。启动后每隔 `scan-interval-seconds` 从已经连上的枪里随机挑一台，发送一条 `SKU-xxxxx` / `A-0x-0x` 扫码。

页面上的「记一笔扫码」调用 `POST /api/simulator/scans`，最后进到 `DeviceSimulator.publishScan`。模拟器没开时这个接口返回 409，`code` 为 `SIMULATOR_DISABLED`。指定的枪还没连上时返回 404，`code` 为 `DEVICE_OFFLINE`。
