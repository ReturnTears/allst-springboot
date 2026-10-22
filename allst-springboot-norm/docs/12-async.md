# 12. 异步处理

主流程提交成功后再做通知；耗时统计放到业务线程池，容器线程不必一直占着。

## 线程池

`AsyncConfig` 注册 `normExecutor`：核心 4，最大 16，队列 200。队列满时由调用线程自己执行，避免通知被静默丢弃。任务会拷贝 MDC，异步日志里仍有 `traceId`。

## 两种用法

**事务提交后通知。** `UserCommandService` 和 `OrderCommandService` 在事务提交后调用 `NotifyService`。`NotifyService` 的方法标了 `@Async("normExecutor")`。事务回滚时不会发通知。当前实现把通知打到日志里，替换成短信或消息队列时保持这个边界即可。

**请求内异步。** `GET /api/v1/reports/summary` 返回 `CompletableFuture`。`ReportService` 在 `normExecutor` 里统计用户数和订单数。Spring MVC 在任务完成前释放容器线程。访问日志等最终状态出来后再记录。

异步第二次分发不再做签名和白名单校验，这两项在第一次请求时已经完成。

## 生产注意

- `@Async` 方法必须是 public，并且从另一个 Spring Bean 调用。同类内部自调用不会走代理。
- 线程池大小按 CPU 和下游耗时调整。通知如果变慢，先扩队列或改成消息队列，不要让请求线程同步等待外部短信网关。
- 异步任务里的异常要自己捕获。`ApiLogService` 落库失败只记警告，不反过来影响已经返回的接口。
