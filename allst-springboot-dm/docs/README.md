# 文档索引

每篇只讲一件事，并写明对应的类。改代码时以这里的类名为准回溯。

| 编号 | 主题 | 文档 | 主要代码 |
| --- | --- | --- | --- |
| 1 | 目标与读法 | [01-goal-and-approach.md](01-goal-and-approach.md) | `showcase.PlaceOrderFacade` |
| 2 | 场景速查表 | [02-scenario-map.md](02-scenario-map.md) | `showcase.PatternCatalog` |
| 3 | 创建型 5 种 | [03-creational.md](03-creational.md) | `creational.*` |
| 4 | 结构型 7 种 | [04-structural.md](04-structural.md) | `structural.*`、`showcase.PlaceOrderFacade` |
| 5 | 行为型 11 种 | [05-behavioral.md](05-behavioral.md) | `behavioral.*` |
| 6 | Spring 里的模式与坑 | [06-spring-patterns.md](06-spring-patterns.md) | `support.*`、`structural.proxy.*` |
| 7 | MySQL 8 / Redis | [07-mysql-redis.md](07-mysql-redis.md) | `domain.OrderRecord`、`support.KeyValueStore` |
| 8 | 反模式与选型 | [08-anti-patterns.md](08-anti-patterns.md) | 见该篇表格 |
| 9 | 运行与接口 | [09-api-and-run.md](09-api-and-run.md) | `showcase.DmShowcaseController` |
| 10 | 开源项目参考 | [10-references.md](10-references.md) | — |
| 11 | 元数据驱动的多方言 DDL 生成 | [11-ddl-generation.md](11-ddl-generation.md) | `ddl.*` |

总览和启动方式见模块根目录 [README.md](../README.md)。

## 三种读法

- 带着需求来：直接查 [02](02-scenario-map.md)，找最接近的场景，按「对应类」跳到代码。
- 系统学一遍：按 [03](03-creational.md) → [04](04-structural.md) → [05](05-behavioral.md) 顺序读，再读 [06](06-spring-patterns.md) 看框架怎么替你做掉一部分。
- 准备落地：先读 [08](08-anti-patterns.md) 确认不该用，再读 [07](07-mysql-redis.md)、[09](09-api-and-run.md) 跑起来验证。
