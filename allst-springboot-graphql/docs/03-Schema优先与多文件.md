# 03 Schema 优先与多文件

Schema 放在 `src/main/resources/graphql/`。

- `books.graphqls`：原来的书和作者
- `subscription.graphqls`：分类、读者、订阅，以及日期标量

`application.properties` 里已经有：

```properties
graphql.tools.schema-location-pattern=**/*.graphqls
```

Kickstart 会把这些文件收成一个 schema。新增字段用 `extend type`，这样不用改原来的类型定义：

```graphql
extend type Book {
    category: Category
}

extend type Query {
    deskStats: DeskStats!
    memberships(filter: MembershipFilter, page: PageQuery): MembershipPage!
}

extend type Mutation {
    createMembership(input: MembershipInput!): Membership!
}
```

根上的 `Subscription` 是 GraphQL 的实时操作，和业务表 `membership` 不是一回事。业务记录在 schema 里叫 `Membership`，避免和根类型 `Subscription` 重名。

类型上能写说明的地方使用了描述字符串，GraphiQL 文档面板能直接看到。

IDE 的 `graphql.config.yml` 指向 `src/main/resources/graphql/*.graphqls`，方便跳转。它不参与运行。
