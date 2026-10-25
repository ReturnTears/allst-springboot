# 状态机规范示例

这个模块用一条跨境订单履约主线，说明复杂业务在引入状态机之后是怎样被拆开的。另外补了两个场景：合同的法务/财务并行审核，以及发票这种更适合轻量状态机的流程。

运行时是 Java 11、Spring Boot 2.6.5。状态机使用 Spring Statemachine 2.5.1，和 Boot 2 的阻塞式 API 对齐。3.x 改成了 Reactor，4.x 面向 Boot 3，这里不跟。发票场景使用 COLA 4.3.2 的无状态状态机。

## 文档

从 [docs/README.md](docs/README.md) 进入。每篇对应一块规范，代码类名以文档里写的为准。

| 文档 | 内容 |
| --- | --- |
| [01-pain-and-target.md](docs/01-pain-and-target.md) | 简化前的写法，以及要拆掉的四类复杂度 |
| [02-simplify-steps.md](docs/02-simplify-steps.md) | 订单履约是怎样一步步收进状态机的 |
| [03-trade-order.md](docs/03-trade-order.md) | 状态、事件、守卫、动作和类的对应关系 |
| [04-design-patterns.md](docs/04-design-patterns.md) | 用到的设计模式和各自解决的问题 |
| [05-persistence-and-threads.md](docs/05-persistence-and-threads.md) | 持久化、同单串行、线程池、并行预检 |
| [06-contract-regions.md](docs/06-contract-regions.md) | 补充场景：合同双轨并行审核 |
| [07-invoice-cola.md](docs/07-invoice-cola.md) | 补充场景：发票，以及什么时候不要用 Spring Statemachine |
| [08-conventions.md](docs/08-conventions.md) | 使用约定和本示例里踩过的坑 |
| [09-api.md](docs/09-api.md) | 接口和一条完整调用顺序 |

参考的开源实现：

- [spring-projects/spring-statemachine](https://github.com/spring-projects/spring-statemachine)，主要对照 `persist`、`showcase`、`tasks`（fork/join）这几个示例的机器生命周期，没有照搬它们的业务。
- [alibaba/COLA 的 cola-component-statemachine](https://github.com/alibaba/COLA/tree/master/cola-components/cola-component-statemachine)，发票定义按它的 Builder 写法。

## 启动

MySQL 用户 `root`，密码为空。库名 `allst_state_machine` 会在第一次连接时创建，表由 JPA `ddl-auto: update` 生成。

```text
mvn -pl allst-springboot-state-machine spring-boot:run
```

端口 `8129`。接口说明在 `http://localhost:8129/swagger-ui.html`。

启动后会写入两笔演示库存：`SKU-PHONE` 100 件，`SKU-GIFT` 2 件。

测试不连 MySQL，使用内存库：

```text
mvn -pl allst-springboot-state-machine test
```

模块目录下也可以用 `mvnw.cmd test`。
