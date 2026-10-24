# 10 DataLoader

一次查询如果返回 20 笔订阅，再为每笔订阅各查一次读者和书，就会变成 1 + 20 + 20 次 SQL。这是 N+1。

新的关联字段使用 DataLoader。同一轮字段解析里，相同名字的加载器会把编号收齐，然后各打一次 `IN` 查询。

加载器在 `LoaderRegistryFactory` 里注册：

| 名称 | 批量内容 |
| --- | --- |
| `readerById` | 订阅上的读者 |
| `bookById` | 订阅上的书 |
| `categoryByBookId` | 书的分类 |
| `bookCountByCategoryId` | 每个分类下的书数量 |
| `membershipsByReaderId` | 读者展开的订阅列表 |

`DataLoaderContextBuilder` 为每个 HTTP 请求、每个 WebSocket 会话新建一个 `DataLoaderRegistry`。注册表不能做成单例，否则不同请求的缓存会串在一起。

批量函数放在 `CompletableFuture.completedFuture` 里同步执行，这样仍在当前请求线程上访问 JPA，不把 EntityManager 带到别的线程。

原来的 `BookAuthorResolver.getAuthor` 仍是按书逐条查询。对比方式：在 GraphiQL 里同时查询 `findAll { author { id } category { id } }`，作者查询会按书的数量出现，分类查询是一批。

字段解析器通过 `DataFetchingEnvironment.getDataLoader` 取加载器，返回 `CompletableFuture`。GraphQL Java 会在这一层结束后统一 dispatch。
