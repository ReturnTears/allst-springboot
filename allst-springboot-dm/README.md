# Spring Boot 业务项目中的设计模式实录

这个模块记录的不是 23 个孤立的模式玩具，而是**一条电商下单链路里，设计模式是怎么一起出现的**：请求进来先过校验链，用规则树和表达式判断优惠资格，用策略算价，用建造者固化规则，用抽象工厂取一整套渠道组件，用享元读 SKU 规格，用工厂方法创建支付命令，用状态机推进订单状态，再用命令落库、事件通知、备忘录留痕、适配器对物流、访问者换口径重算……

每种模式都配了可运行的 Spring Boot 代码、对应的类名和单元测试。文档回答三个问题：**什么场景该用哪个模式**、**不用它会怎样**、**用了主要解决什么问题**；也写了什么时候不该用。

运行时 Java 11、Spring Boot 2.6.5。落库用 MySQL 8（用户 `root`、密码为空，库 `allst_dm` 首次连接自动创建，表由 JPA `ddl-auto: update` 生成）。Redis 是可选实现，默认关闭，不装 Redis 也能启动和测试。

## 文档

从 [docs/README.md](docs/README.md) 进入，每篇只讲一件事，代码类名以文档里写的为准。

| 文档 | 内容 |
| --- | --- |
| [01-goal-and-approach.md](docs/01-goal-and-approach.md) | 模块目标、怎么读一个模式、下单主线的调用顺序 |
| [02-scenario-map.md](docs/02-scenario-map.md) | **场景速查表**：需求 → 模式 → 解决的问题 → 对应类 → 开源参考 |
| [03-creational.md](docs/03-creational.md) | 创建型 5 种：单例、工厂方法、抽象工厂、建造者、原型 |
| [04-structural.md](docs/04-structural.md) | 结构型 7 种：适配器、桥接、组合、装饰器、外观、享元、代理 |
| [05-behavioral.md](docs/05-behavioral.md) | 行为型 11 种：责任链、命令、解释器、迭代器、中介者、备忘录、观察者、状态、策略、模板方法、访问者 |
| [06-spring-patterns.md](docs/06-spring-patterns.md) | Spring 自己实现的模式，以及容器里的坑（作用域、自调用、事件时机） |
| [07-mysql-redis.md](docs/07-mysql-redis.md) | MySQL 8 与 Redis 在示例里的落点、配置、测试策略 |
| [08-anti-patterns.md](docs/08-anti-patterns.md) | 什么时候不要用、常见滥用与选型决策 |
| [09-api-and-run.md](docs/09-api-and-run.md) | 启动、接口清单、curl 演练、测试验收 |
| [10-references.md](docs/10-references.md) | 参考的 GitHub 开源项目与场景对照，标注核实程度 |
| [11-ddl-generation.md](docs/11-ddl-generation.md) | **新实战**：元数据驱动的多方言 DDL 生成（策略+能力模型+模板方法+Stream，10 方言导出 SQL） |

## 快速开始

```text
mvn -pl allst-springboot-dm spring-boot:run
```

端口 `8367`。启动前确保本机 MySQL 8 可用（root / 空密码）；库会自动创建。

```bash
curl -X POST http://localhost:8367/api/dm/orders \
  -H "Content-Type: application/json" \
  -d '{"buyer":"tester","sku":"SKU-PHONE","unitPrice":199.00,"quantity":2,
       "vip":true,"payChannel":"ALIPAY","channel":"DOMESTIC",
       "requestId":"demo-001","riskScore":10}'
```

返回的 `data.trace` 会逐条列出这次请求经过了哪些模式。其它常用入口：

- `GET /api/dm/patterns`：26 条「场景 → 模式 → 解决的问题」速查表，和 [02](docs/02-scenario-map.md) 同源。
- `GET /api/dm/demo/{pattern}`：单独运行某一个模式，23 个 key 见 [09](docs/09-api-and-run.md)。
- `GET /api/dm/stats`：规则缓存、享元池、装饰链命中、AOP 计时、事件轨迹等运行痕迹。
- `GET /hello/{name}`：脚手架自带的探针，没改动。

测试不连 MySQL、不连 Redis，用 H2 内存库：

```text
mvn -pl allst-springboot-dm test
```

## 代码结构

```text
src/main/java/com/allst/springboot
├── common/          统一响应与异常（外观）
├── domain/          订单请求、状态、JPA 实体、仓储
├── support/         KeyValueStore（内存 / Redis 两种实现）
├── creational/      创建型 5 种
├── structural/      结构型 6 种（外观在 showcase）
├── behavioral/      行为型 11 种
├── showcase/        下单外观、模式目录、演示接口
└── ddl/             元数据驱动的多方言 DDL 生成（见 11-ddl-generation.md）
    ├── meta/        TableMeta / ColumnMeta（建造者+JPA）、仓储、种子
    ├── dialect/     DdlDialect 策略 + 能力模型 + 模板基类 + 路由 + 10 方言
    ├── generation/  DdlGenerator 门面（Stream 编排）+ 文件导出
    └── api/         DdlController（能力矩阵/生成/导出/下载）
```

`showcase.PlaceOrderFacade` 是主线，`showcase.PatternCatalog` 是速查表的代码版本，`showcase.DemoRunner` 让每个模式都能单独调用。

## 参考的开源项目

业务场景对照了 [macrozheng/mall](https://github.com/macrozheng/mall)（电商订单、促销、库存）、[alibaba/Sentinel](https://github.com/alibaba/Sentinel)（slot chain 责任链）、[apache/dubbo](https://github.com/apache/dubbo)（SPI 扩展点）、[mybatis/mybatis-3](https://github.com/mybatis/mybatis-3)（Mapper 代理、CachingExecutor 装饰器）、[spring-projects/spring-framework](https://github.com/spring-projects/spring-framework)（JdbcTemplate、事件、AOP）、[alibaba/COLA](https://github.com/alibaba/COLA)（状态机与扩展点）等。对照的是它们的业务问题和结构，没有照搬源码；核实程度见 [10-references.md](docs/10-references.md)。
