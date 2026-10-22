# 15 任务调度

代码：`schedule/DelayTaskService`、`schedule/ScheduleController`。脚本：`resources/lua/claim_delay.lua`。

定时任务如果只放在某一个应用节点的内存里，节点一挂任务就丢，多节点又会重复执行。延迟任务放在 ZSET `schedule:delay` 里：成员是任务 JSON，分数是执行时间的毫秒值。

调度器每秒执行一次 Lua：取出分数不大于当前时间的任务，并用 `ZREM` 删掉。只有删成功的一方才算领到。多个节点可以同时跑这段调度，同一条任务只会有一方执行。

支持两种任务：

- `LOG`：把任务记到 `schedule:result`
- `DELETE_KEY`：删除一个 `temp:` 开头的键，避免调度接口变成任意删键

Redisson 也有 `RDelayedQueue`。这里用 ZSET，是为了把领取过程露在脚本里。失败重试、任务分片和错过的补偿，生产上要另做；这个队列是至少一次，任务本身要能重复执行。

真正的 cron 表达式、错过触发补偿，仍然可以用 Quartz。Redis 适合「几秒或几小时后执行一次」这种延迟任务。

## 接口

- `POST /api/schedule/tasks`，体：`{"type":"LOG","payload":"发送周报","delaySeconds":5}`
- `GET /api/schedule/tasks` 尚未到期的任务
- `GET /api/schedule/results` 已执行的任务

## 验证

提交一个 5 秒后的 LOG。马上看待执行，能看到它；6 秒后再看，待执行里没有了，已完成里有 `status=logged`。

先用第 09 章写一个 `temp:` 键，再提交 `DELETE_KEY`，到期后那个键应消失。
