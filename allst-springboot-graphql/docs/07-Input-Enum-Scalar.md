# 07 Input、Enum、Scalar

## Input

写入和筛选使用 input 类型，对应 `com.allst.boot.subscription.graphql.input` 里的同名类。Kickstart 按类的简单名匹配，字段名要和 schema 一致。

空着的可选字段在 Java 里是 `null`。更新时 `null` 表示不改。字符串如果传了空串，服务层会把它收成 `null`，用来清空备注、说明或电话。

校验放在服务里，不依赖把实体直接暴露给 GraphQL。不通过时抛 `BusinessException`，例如姓名长度、邮箱、电话位数、重复名称。

## Enum

Schema 枚举和 Java 枚举同名：

- `ReaderStatus`
- `MembershipPlan`
- `MembershipStatus`
- `SortDirection`

数据库用 `@Enumerated(EnumType.STRING)` 保存枚举名。不要用序号，否则调整枚举顺序会把旧数据读错。

`MembershipPlan` 上还带了天数和价格。这两个值不是 GraphQL 枚举的字段，而是 Java 里的属性。页面通过 `planQuotes` 读取，避免前后端各写一份价目表。

## Scalar

`Int`、`String`、`Float`、`Boolean` 使用 GraphQL 内置标量。`Float` 对应 Java 的 `Double`。

日期需要自定义标量，因为内置类型没有 `LocalDate`：

- `Date` → `LocalDate`，格式 `yyyy-MM-dd`
- `DateTime` → `LocalDateTime`，格式 `yyyy-MM-ddTHH:mm:ss`，也接受空格代替 `T`

实现在 `GraphQlScalars`。`ScalarConfiguration` 把它们注册成 `GraphQLScalarType` Bean，Kickstart 会按标量名字挂到 schema 上。

格式不对时，标量解析失败，GraphQL 会在 `errors` 里返回说明，例如“日期格式应为 yyyy-MM-dd”。
