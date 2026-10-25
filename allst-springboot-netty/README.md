# 仓储扫码设备接入网关

这个项目用一套能直接跑起来的仓储场景，把 Netty 在业务里真正承担的四件事放在同一个 Spring Boot 进程里：

- 设备 TCP 长连接服务端
- 扫码枪 TCP 客户端（含模拟器）
- WebFlux HTTP，给 WMS 下发指令、给看板查状态
- WebSocket，给仓管看实时流水

技术栈：Spring Boot 3.4.5、Java 17、Netty（版本由 `spring-boot-starter-webflux` 管理）。HTTP 服务器是 Reactor Netty，设备通道和看板是另外两组自己创建的 Netty。

## 先跑起来

```text
.\mvnw.cmd spring-boot:run
```

浏览器打开 <http://localhost:8080>。

| 用途 | 地址 |
| --- | --- |
| 仓管看板、HTTP 接口 | http://localhost:8080 |
| 扫码枪 TCP | `0.0.0.0:9000` |
| 看板 WebSocket | `ws://localhost:9001/ws/dashboard` |

默认会拉起 `WH-SCANNER-01` 到 `WH-SCANNER-03`。它们登录、每 15 秒心跳，大约每 8 秒随机扫一次码。页面底部可以给某一台下发「去库位 / 锁定库位 / 盘点」，也可以让模拟枪补一笔指定条码。

测试：

```text
.\mvnw.cmd test
```

`WarehouseGatewayTest` 会自己起一个随机端口，连上一台设备，完成登录、扫码、下发指令、等待 ACK，并断言 WebSocket 收到了对应消息。

## 文档

按这个顺序回看实现：

1. [业务场景](docs/01-业务场景.md)：为什么是仓储扫码枪，和 CIM、Turms 的对应关系
2. [总体架构](docs/02-总体架构.md)：三个 Netty、一次扫码、一次下发
3. [自定义协议](docs/03-自定义协议.md)：长度字段帧和 JSON 消息
4. [TCP 服务端](docs/04-TCP服务端.md)：Pipeline、会话、空闲断开
5. [TCP 客户端](docs/05-TCP客户端.md)：登录、心跳、断线重连、模拟器
6. [WebFlux 接口](docs/06-WebFlux接口.md)：HTTP API 和 SSE
7. [WebSocket 看板](docs/07-WebSocket看板.md)：握手、快照、页面
8. [生命周期](docs/08-生命周期.md)：SmartLifecycle 的启动和关闭顺序
9. [线程模型](docs/09-线程模型.md)：boss、worker、业务线程、Reactor Netty
10. [自动配置](docs/10-自动配置.md)：`AutoConfiguration.imports` 和条件装配
11. [运行与验证](docs/11-运行与验证.md)：curl、日志里该出现的线程名、端口冲突
