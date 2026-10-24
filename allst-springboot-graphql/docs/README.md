# GraphQL 使用记录

这个模块用一个图书订阅台把 Spring Boot 里常见的 GraphQL 写法串起来。原有的图书、作者示例代码都还在，新的读者、分类和订阅是在旁边加上去的。

| 文档 | 内容 |
| --- | --- |
| [01 项目目标与技术栈](01-项目目标与技术栈.md) | 要示范什么，以及各层用什么库 |
| [02 持久化建模](02-持久化建模.md) | MySQL 表、JPA 实体和约束 |
| [03 Schema 优先与多文件](03-Schema优先与多文件.md) | `.graphqls` 怎么拆，怎样扩展已有类型 |
| [04 Query](04-Query查询.md) | 列表、条件、嵌套、聚合 |
| [05 Mutation](05-Mutation增删改.md) | 增删改和业务约束 |
| [06 字段解析与嵌套](06-字段解析与嵌套.md) | 对象字段不放在表上的解析方式 |
| [07 Input、Enum、Scalar](07-Input-Enum-Scalar.md) | 入参对象、枚举和日期标量 |
| [08 分页、过滤、排序](08-分页过滤排序.md) | `PageQuery` 与条件对象 |
| [09 错误处理](09-错误处理.md) | 业务异常怎么回到 `errors` |
| [10 DataLoader](10-DataLoader.md) | 关联查询为什么要按请求批量加载 |
| [11 实时订阅](11-实时订阅.md) | GraphQL Subscription |
| [12 Vue3 对接](12-Vue3对接.md) | 前端如何发 Query、Mutation 和订阅 |
| [13 运行说明](13-运行说明.md) | 数据库、后端和前端怎么启动 |

GraphiQL：<http://127.0.0.1:8088/graphiql>

订阅台页面：<http://127.0.0.1:5173>
