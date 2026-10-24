# 05 Mutation

修改解析器实现 `GraphQLMutationResolver`。原有方法和新方法可以分在不同的类里。

## 原有写操作

- `newBook(name, pageCount)`：新增书
- `deleteBook(id)`：删除书
- `updateAuthor(id, firstName)`：只改名
- `updateAuthorById(id, firstName, lastName)`：改名和姓

这些方法的参数是散装的。新接口改为一个 `input` 对象，字段以后增加时不用改方法签名。见[Input、Enum、Scalar](07-Input-Enum-Scalar.md)。

## 新的写操作

| 字段 | 规则 |
| --- | --- |
| `updateBook` | 改书名或页数。书名重复时拒绝 |
| `assignBookCategory` / `clearBookCategory` | 给书指定或去掉分类 |
| `createCategory` / `updateCategory` / `deleteCategory` | 分类增删改。分类下还有书时不能删 |
| `createReader` / `updateReader` / `deleteReader` | 读者增删改。还有订阅时不能删读者 |
| `createMembership` | 新建订阅 |
| `updateMembership` | 改方案、状态、备注。改方案时按原开始日期重算结束日期和价格 |
| `cancelMembership` | 退订。已经退订则原样返回 |
| `deleteMembership` | 删除记录 |

新建订阅时：

1. 读者必须存在，且状态是 `ACTIVE`。
2. 书必须存在。
3. 同一读者、同一本书不能同时有两条 `ACTIVE` 或 `PAUSED` 的订阅。退订或到期之后可以再订。
4. 不传开始日期时用当天。结束日期和价格由 `MembershipPlan` 决定。

```graphql
mutation ($input: MembershipInput!) {
  createMembership(input: $input) {
    id
    plan
    status
    startDate
    endDate
    price
  }
}
```

```json
{
  "input": {
    "readerId": 3,
    "bookId": 2,
    "plan": "MONTHLY",
    "remark": "试读"
  }
}
```

删除有订阅的书、删除仍被引用的分类或读者，会得到 `REFERENCED` 或更具体的业务说明，而不是把数据库异常原文抛给页面。
