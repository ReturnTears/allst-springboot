# Quartz
```text
Quartz 的核心类有以下三部分：

任务 Job ： 需要实现的任务类，实现 execute() 方法，执行后完成任务。
触发器 Trigger ： 包括 SimpleTrigger 和 CronTrigger。
调度器 Scheduler ： 任务调度器，负责基于 Trigger触发器，来执行 Job任务。

JobDetail 的作用是绑定 Job，是一个任务实例，它为 Job 添加了许多扩展参数。

每次Scheduler调度执行一个Job的时候，首先会拿到对应的Job，然后创建该Job实例，再去执行Job中的execute()的内容，
任务执行结束后，关联的Job对象实例会被释放，且会被JVM GC清除。
https://zhuanlan.zhihu.com/p/522284183
```

