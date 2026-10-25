# WebFlux 接口

## 为什么 HTTP 也是 Netty

`pom.xml` 只引入 `spring-boot-starter-webflux`，没有 `spring-boot-starter-web`。

Spring Boot 在 classpath 上同时看到 `DispatcherServlet` 和 WebFlux 时，会把应用类型判成 Servlet，内嵌 Tomcat，WebFlux 只剩下 `WebClient` 这类客户端能力。本项目要观察的是：HTTP 那一组 Netty（Reactor Netty）和设备那一组 Netty 同时存在，但不是同一个 `EventLoopGroup`。

对应的自动配置类在 `spring-boot-autoconfigure` 里：

- `ReactiveWebServerFactoryAutoConfiguration`
- `ReactiveWebServerFactoryConfiguration.EmbeddedNetty`

后者在 classpath 上有 Reactor Netty、且没有别的 `ReactiveWebServerFactory` 时，注册 `NettyReactiveWebServerFactory`。启动日志里的 `o.s.b.w.e.netty.NettyWebServer` 就是这套服务器。它的 `SmartLifecycle` phase 在 Spring Boot 3.4.5 里是 `2147481599`，比设备服务和模拟器都晚启动、更早停止。

静态页面来自 `src/main/resources/static/index.html`，由 WebFlux 的资源处理器提供。`spring.web.resources.cache.cachecontrol.no-cache=true`，改完页面刷新就能看到。

## 接口

控制器是 `DeviceConsoleController`。下发指令不等待设备，所以用 202。

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/meta` | 看板用来发现 TCP 端口、WebSocket 端口和模拟器是否开启 |
| GET | `/api/devices` | 当前会话，按设备号排序 |
| GET | `/api/devices/{deviceId}` | 一台设备。不在线是 404 |
| POST | `/api/devices/{deviceId}/commands` | 下发指令，202，体是 `CommandRecord`，初始状态 `SENT` |
| GET | `/api/commands/{requestId}` | 查 `SENT` / `ACKED` / `FAILED` |
| GET | `/api/events?limit=50` | 最近扫码，新的在前，`limit` 最大 200 |
| GET | `/api/events/stream` | `text/event-stream`，订阅 `RealtimePushService` 的回放流 |
| POST | `/api/simulator/scans` | 让模拟枪发一笔指定扫码 |

指令体：

```json
{ "action": "MOVE", "params": { "aisle": "A-03" } }
```

`action` 不能为空白。写缓冲不可写时返回 429，`code` 为 `DEVICE_BUSY`。

SSE 使用 `Sinks.many().replay().limit(200)`。后连上的订阅者会先收到最近最多 200 条，再接实时消息。并发 `tryEmitNext` 遇到 `FAIL_NON_SERIALIZED` 时会短暂重试，仍失败就丢掉这一条并打 debug 日志，避免卡住设备线程。

## 错误体

`ApiExceptionHandler` 统一成：

```json
{ "code": "DEVICE_OFFLINE", "message": "设备不在线: WH-SCANNER-09", "timestamp": 1760000000000 }
```

| 异常 | HTTP | code |
| --- | --- | --- |
| `DeviceOfflineException` | 404 | `DEVICE_OFFLINE` |
| `DeviceBusyException` | 429 | `DEVICE_BUSY` |
| `SimulatorDisabledException` | 409 | `SIMULATOR_DISABLED` |
| 校验失败、JSON 无法解析 | 400 | `BAD_REQUEST` |
| `ResponseStatusException`（含静态资源 404） | 原状态码 | `HTTP_ERROR` |
| 其他异常 | 500 | `INTERNAL` |

500 只把固定文案返回给调用方，堆栈留在服务端日志。
