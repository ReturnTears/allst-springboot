# 04 Query

查询解析器实现 `GraphQLQueryResolver`。可以有多个这种 Bean，Kickstart 会把方法合并到根类型 `Query` 上。方法名就是字段名。

## 原有查询

`BookQuery`、`AuthorQuery` 仍提供：

- `findAll`：全部书
- `getBookByName(name)`
- `allAuthor`
- `getAuthorByBookId(bookId)`
- `getAuthorById(id)`

嵌套作者不在 `BookQuery` 里查，而在 `BookAuthorResolver`。见[字段解析](06-字段解析与嵌套.md)。

```graphql
query {
  findAll {
    id
    name
    pageCount
    author { id firstName lastName }
    category { id name }
  }
}
```

`category` 是后加的字段，旧查询不写它也仍然合法。

## 新增查询

| 字段 | 作用 |
| --- | --- |
| `bookById(id)` | 按编号取一本书，没有则报错 |
| `searchBooks(keyword)` | 按书名包含关系过滤，关键字为空时返回全部 |
| `categories` / `category(id)` | 分类列表和单条 |
| `readers(filter, page)` | 读者分页 |
| `reader(id)` | 单个读者 |
| `memberships(filter, page)` | 订阅分页 |
| `membership(id)` | 单笔订阅 |
| `readerMemberships(readerId)` | 某个读者的订阅，按创建时间倒序 |
| `planQuotes` | 方案、天数和价格，前端下拉框用这一份 |
| `deskStats` | 书、作者、读者、分类、在订数量 |

聚合查询直接数表，不把实体拉到内存里再 `size()`：

```graphql
query {
  deskStats {
    bookCount
    authorCount
    readerCount
    categoryCount
    activeMembershipCount
  }
}
```

`activeMembershipCount` 只数状态为 `ACTIVE` 的订阅。

按编号查不到记录时抛业务异常，不返回 `null`。列表没有数据时返回空数组。这个约定记在[错误处理](09-错误处理.md)。
