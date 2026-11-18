# 用 Stream 高效编码：从「会转换集合」到「会编排业务」

> 你已经会用 `map` / `filter` / `collect` 处理列表——这是**第一层**。
> 本文带你走完剩下五层，每层都有可运行代码 + 测试 + 「什么时候该这么写」。
>
> 模块：`allst-springboot-stream`｜Java 11｜Spring Boot 2.6.5｜H2 内存库（开箱即跑）
> 验证：`mvn -pl allst-springboot-stream test`（44 个测试全绿）
> 运行：启动 `StreamApplication`，访问 `http://localhost:8710/api/stream/...`

---

## 目录

- [0. 先建立一个认知：Stream 不是语法糖](#0-先建立一个认知stream-不是语法糖)
- [1. 第一层跨越：四大函数式接口是「可传递的行为」](#1-第一层跨越四大函数式接口是可传递的行为)
- [2. Supplier：把「要不要算」的决定权交给调用方](#2-supplier把要不要算的决定权交给调用方)
- [3. Consumer：把遍历骨架写一次](#3-consumer把遍历骨架写一次)
- [4. Function 与 Predicate：组合优于嵌套](#4-function-与-predicate组合优于嵌套)
- [5. 函数式策略模式：干掉 if-else 链](#5-函数式策略模式干掉-if-else-链)
- [6. 函数回调模板：通用骨架 + 业务填空](#6-函数回调模板通用骨架--业务填空)
- [7. 高级收集器：一次遍历出报表](#7-高级收集器一次遍历出报表)
- [8. 流机制与六个真实陷阱](#8-流机制与六个真实陷阱)
- [9. 数据库实战：用 Stream 消除 N+1](#9-数据库实战用-stream-消除-n1)
- [10. 什么时候「不该」用 Stream](#10-什么时候不该用-stream)
- [11. 好处总览与场景速查表](#11-好处总览与场景速查表)
- [12. 学习路径与练习建议](#12-学习路径与练习建议)
- [附录：代码地图](#附录代码地图)

---

## 0. 先建立一个认知：Stream 不是语法糖

很多人把 Stream 当「for 循环的简写」，所以只用到转换。真正的价值在三点：

| 维度 | for 循环 | Stream + 函数式接口 |
| --- | --- | --- |
| **行为能否传递** | 不能，逻辑焊死在循环体里 | 能，`Predicate`/`Function` 是值，可存、可传、可组合 |
| **执行时机** | 立即，全部算完 | 惰性 + 短路，只算必要的部分 |
| **扩展方式** | 改原方法（违反开闭原则） | 加一个 lambda，原代码不动 |

**一句话**：Stream 让你把「做什么」和「怎么做」分开。前者是业务，后者是骨架。
骨架写一次复用 everywhere，业务用 lambda 填空——这就是你看到的「优雅编排」的本质。

### 本模块的六层结构

```
第 1 层  四大接口          functional/     ← 行为参数化
第 2 层  函数式策略        strategy/       ← 消除 if-else
第 3 层  回调模板          callback/       ← 骨架与业务分离
第 4 层  高级收集器        collector/      ← 一次遍历出报表
第 5 层  流机制与陷阱      mechanics/      ← 写对，不出事
第 6 层  数据库实战        repository/     ← 消除 N+1（收益最大）
```

领域模型统一用**订单**（`domain/Order`），贯穿全部案例，便于对照。

---

## 1. 第一层跨越：四大函数式接口是「可传递的行为」

| 接口 | 签名 | 语义 | 你已在用 | 进阶用法 |
| --- | --- | --- | --- | --- |
| `Function<T,R>` | `R apply(T)` | 转换 | `map(Order::amount)` | `andThen`/`compose` 组合、`toMap` 建索引 |
| `Predicate<T>` | `boolean test(T)` | 判断 | `filter(o -> ...)` | `and`/`or`/`negate` 组合成可配置条件 |
| `Consumer<T>` | `void accept(T)` | 消费 | `forEach(System.out::println)` | `andThen` 串加工步骤、装饰器做失败隔离 |
| `Supplier<T>` | `T get()` | 生产 | 较少 | 延迟求值、兜底链、记忆化、`generate` |

**关键转变**：不要把它们只当 lambda 的「类型标注」，要当**可以存进集合、当参数传、当字段存的值**。

```java
// 你现在可能的写法：条件焊死在方法里
public List<Order> query(String city, BigDecimal min) {
    return orders.stream()
        .filter(o -> city == null || city.equals(o.getCity()))
        .filter(o -> min == null || o.amount().compareTo(min) >= 0)
        .collect(toList());
}

// 进阶：条件是「值」，由调用方组装
public List<Order> filter(List<Order> orders, List<Predicate<Order>> conditions) {
    Predicate<Order> combined = conditions.stream().reduce(o -> true, Predicate::and);
    return orders.stream().filter(combined).collect(toList());
}
```

后者的好处：新增查询维度**不用改这个方法**；每个条件能单独单测；条件能从配置/前端动态生成。

---

## 2. Supplier：把「要不要算」的决定权交给调用方

📁 `functional/SupplierShowcase.java`

### 2.1 多级数据源兜底（最实用）

**场景**：查订单先走本地缓存 → Redis → 数据库，命中就不往下查。

```java
// ❌ 反模式：传列表，三个来源全查一遍（缓存命中也白查两次库）
List<Order> find(List<Order> cache, List<Order> redis, List<Order> db) { ... }

// ✅ 传 Supplier，靠惰性求值短路
@SafeVarargs
public final List<Order> firstNonEmpty(Supplier<List<Order>>... sources) {
    return Stream.of(sources)
            .map(Supplier::get)          // 惰性：只有需要时才调用
            .filter(list -> list != null && !list.isEmpty())
            .findFirst()                 // 短路：命中即停
            .orElseGet(List::of);
}
```

**测试证明**（`FunctionalInterfaceTest.supplierShortCircuitsLaterSources`）：
缓存命中时 `dbCalls == 0`——数据库那次查询**根本没发生**。

> **这是 Supplier 最核心的价值**：参数从「值」变成「取值的方法」，
> 于是「要不要取、取几次」由接收方决定。省掉的是真实 IO，不是几行代码。

### 2.2 记忆化：昂贵计算只算一次

**场景**：一次报表请求里，多个指标都要用「当月总成交额」。

```java
public <T> Supplier<T> memoize(Supplier<T> delegate) {
    Map<String, T> holder = new ConcurrentHashMap<>(1);
    return () -> holder.computeIfAbsent("value", key -> delegate.get());
}
```

调用三次 `cached.get()`，底层只算一次（测试 `memoizeComputesOnce` 断言 `calls == 1`）。

### 2.3 `orElse` vs `orElseGet`：求值时机的坑

```java
// ❌ orElse 的参数总会被求值，哪怕 Optional 有值
optional.orElse(queryFromDb());        // queryFromDb() 一定执行！

// ✅ orElseGet 只在为空时才调用
optional.orElseGet(() -> queryFromDb());
```

测试 `orElseGetIsLazy` 验证：有值时兜底调用次数为 0。
**这是线上常见的隐性性能问题**——看起来只是取默认值，实际每次都查了库。

### 2.4 `computeIfAbsent` 替代 containsKey + put

```java
// ❌ 三次哈希查找
if (!map.containsKey(city)) map.put(city, BigDecimal.ZERO);
map.put(city, map.get(city).add(amount));

// ✅ 一次搞定，意图更明确
map.merge(city, amount, BigDecimal::add);
```

### 2.5 `Stream.generate`：无限流必须 limit

```java
AtomicInteger seq = new AtomicInteger();
Supplier<String> supplier = () -> prefix + String.format("%04d", seq.incrementAndGet());
return Stream.generate(supplier).limit(count).collect(toList());
```

**场景**：批量造测试数据、预生成券码。
⚠️ 忘了 `limit` 就是死循环——`generate` 是无限流。

**与 `iterate` 的区别**：`generate` 无状态依赖（每次独立产生）；`iterate` 有种子、按前值推导。

---

## 3. Consumer：把遍历骨架写一次

📁 `functional/ConsumerShowcase.java`

### 3.1 `andThen` 串成加工流水线

**场景**：订单对外输出前要「脱敏 → 抹券码 → 补字段」。

```java
public List<Order> enrich(List<Order> orders, List<Consumer<Order>> steps) {
    Consumer<Order> combined = steps.stream().reduce(order -> { }, Consumer::andThen);
    orders.forEach(combined);   // 一次遍历完成全部加工
    return orders;
}

// 调用方决定装哪几步
enrich(orders, List.of(maskMemberName(), clearCoupon()));
```

**好处**：
- 对比三个 for 循环：只遍历一次
- 对比一个循环塞三段逻辑：每步**可独立测试、可按需组合**
- 新增一步只是往列表加个 lambda，遍历代码不动

### 3.2 失败隔离装饰器（批处理刚需）

**场景**：数据同步 10 万条，一条脏数据不能让整批失败。

```java
public <T> int forEachSafely(List<T> items, Consumer<T> action) {
    int[] failures = {0};
    items.forEach(item -> {
        try {
            action.accept(item);
        } catch (RuntimeException ex) {
            failures[0]++;
            log.warn("单条处理失败，已跳过: {}", ex.getMessage());
        }
    });
    return failures[0];
}
```

**为什么必须这样**：`Consumer.accept` **不声明受检异常**，所以异常处理只能靠装饰器包一层。
这是函数式接口在真实项目里最常见的「适配」需求。

测试 `forEachSafelyIsolatesFailures`：3 条数据 1 条坏，成功的 2 条照常处理，返回失败数 1。

### 3.3 ⚠️ Consumer 的边界：别在 map 里改对象

```java
// ❌ 反模式：map 语义是「转换」，却在这里产生副作用
.map(o -> { o.setStatus(DONE); return o; })

// ✅ 转换就返回新值
.map(o -> new OrderView(...))

// ✅ 要改原对象就明确用 forEach
orders.forEach(o -> o.setStatus(DONE));
```

`peek` 只适合调试观察，不要用它做业务副作用（某些优化下 `peek` 可能被跳过）。

---

## 4. Function 与 Predicate：组合优于嵌套

📁 `functional/FunctionShowcase.java`、`functional/PredicateShowcase.java`

### 4.1 `andThen` / `compose` 的方向

```java
f.andThen(g)   // 先 f 后 g：g(f(x))
f.compose(g)   // 先 g 后 f：f(g(x))
```

**场景**：导出报表要「补齐会员等级 → 转视图」。

```java
Function<Order, Order> fillLevel = order -> { ... };
Function<Order, OrderView> toView = order -> OrderView.builder()...build();
return fillLevel.andThen(toView);      // 组合后整体传给 map
```

好处：两步各自可测，也能单独复用其中一步。

### 4.2 `toMap` 建索引：把 O(n×m) 降成 O(n+m)

```java
Map<Long, Member> index = members.stream()
        .collect(Collectors.toMap(
                Member::getId,
                Function.identity(),
                (existing, replacement) -> replacement));  // ← 合并函数必须给！
```

**两个必知的坑**：

| 坑 | 后果 | 解法 |
| --- | --- | --- |
| 不给合并函数 | 键重复抛 `IllegalStateException` | 显式写 `(a,b) -> a` 或 `(a,b) -> b`，表达「以谁为准」 |
| value 为 null | `NullPointerException`（HashMap.merge 限制） | value 用对象本身，别用可能为 null 的字段 |

测试 `indexByHandlesDuplicateKeys` 专门验证重复键场景。

### 4.3 Predicate 组合：单位元的妙用

```java
// and 组合，单位元恒真 → 空条件列表 = 不过滤
Predicate<Order> combined = conditions.stream().reduce(o -> true, Predicate::and);

// or 组合，单位元恒假 → 空规则列表 = 全不命中
Predicate<Order> any = rules.stream().reduce(o -> false, Predicate::or);
```

**好处**：不用特判「条件为空」，语义自然。
**场景**：后台列表查询条件由前端动态传；风控「命中任一规则即拦截」。

### 4.4 `partitioningBy`：一次遍历切两份

```java
Map<Boolean, List<Order>> split = orders.stream()
        .collect(Collectors.partitioningBy(predicates.mobile()));
// split.get(true) 命中，split.get(false) 未命中
```

比写两次 `filter` 省一半遍历。**场景**：批量发券要回传「哪些不符合条件」。

### 4.5 `Predicate.not`（Java 11+）

```java
.filter(Predicate.not(order -> order.getStatus().isTerminal()))
```

比 lambda 里写 `!` 更可读，尤其条件本身较长时。

---

## 5. 函数式策略模式：干掉 if-else 链

📁 `strategy/DiscountStrategy.java`、`strategy/DiscountService.java`

### 5.1 传统写法的痛

```java
// ❌ 每加一种券就要改这个方法
if ("NEW10".equals(coupon)) { ... }
else if ("VIP20".equals(coupon)) { ... }
else if ("FULL500".equals(coupon)) { ... }
```

传统策略模式要「1 个接口 + N 个实现类 + 1 个工厂」，十几行样板只为包一个算法。

### 5.2 枚举 + Function 字段

```java
public enum DiscountStrategy {
    NEW10("新人立减10元", order -> order.amount().subtract(BigDecimal.TEN)),
    VIP20("会员八折", order -> order.amount().multiply(new BigDecimal("0.8"))),
    FULL500("满500减50", order -> { ... }),
    NONE("无优惠", Order::amount);          // 单位元策略，调用方不用判空

    private final String description;
    private final Function<Order, BigDecimal> calculator;

    public BigDecimal apply(Order order) {
        return calculator.apply(order).max(BigDecimal.ZERO).setScale(2, HALF_UP);
    }

    public static DiscountStrategy of(String couponCode) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(...))
                .findFirst()
                .orElse(NONE);              // 未知券回落原价，不抛异常
    }
}
```

**好处**：
- 策略标识、算法、说明**收在一处**，不用翻三个文件
- 新增券种只加一个枚举常量，**选择逻辑完全不动**（开闭原则）
- 精度处理、下限保护统一在 `apply` 里，不用每个策略各写一遍
- 枚举天然可被 Stream 消费（`Arrays.stream(values())` 直接生成券列表给前端）

### 5.3 适用边界（重要）

| 情况 | 方案 |
| --- | --- |
| 策略**无状态、不依赖注入** | ✅ 枚举 + Function（本例） |
| 策略要查库、调风控、注入 Bean | 用 Spring Bean + `Map<String, Strategy>` 注入 |

Spring 会把同类型 Bean 按名字注入成 Map，这是「可注入版策略模式」，
本模块 `allst-springboot-security` 的 `SecurityCatalog` 用的就是 `List` 注入的同类手法。

### 5.4 策略 + Stream 编排

```java
// 批量算应付总额：选策略 → 算金额 → 求和
return orders.stream().map(this::payable).reduce(BigDecimal.ZERO, BigDecimal::add);

// 按券种统计优惠额：groupingBy + reducing 一次遍历
return orders.stream()
        .filter(o -> o.getCouponCode() != null)
        .collect(groupingBy(Order::getCouponCode,
                reducing(ZERO, o -> o.amount().subtract(payable(o)), BigDecimal::add)));
```

---

## 6. 函数回调模板：通用骨架 + 业务填空

📁 `callback/BatchTemplate.java`

**这就是你提到的「函数回调里用 Stream」**，也是通用框架剥离业务逻辑的标准做法。

### 6.1 四大接口各司其职

```java
public <S, T> BatchResult process(
        Supplier<List<S>> source,        // 数据从哪来（延迟拉取）
        Predicate<S> accept,             // 哪些合格
        Function<S, T> converter,        // 怎么转换
        Consumer<List<T>> saver,         // 落库/发消息
        int batchSize) { ... }
```

骨架固定：**拉取 → 过滤 → 转换 → 分批消费 → 汇总**。
业务只填四个 lambda，且能分别统计 skipped / failed / succeeded。

### 6.2 调用示例

```java
BatchResult result = template.process(
        () -> fetchFromRemote(),                    // Supplier
        row -> row.getAmount() != null,             // Predicate
        row -> toEntity(row),                       // Function
        repository::saveAll,                        // Consumer
        500);
```

**好处**：以前每个导入功能都要重写一遍分批循环、try-catch、计数；
现在骨架一处维护，新增导入功能只写四个 lambda。

测试 `batchTemplateSeparatesSkippedFailedSucceeded` 验证：
5 条输入 → 1 条被过滤、1 条转换失败、3 条成功，三个计数分别正确。

### 6.3 分批：`IntStream.iterate` 三参版（Java 9+）

```java
return IntStream.iterate(0, start -> start < total, start -> start + batchSize)
        .mapToObj(start -> items.subList(start, Math.min(start + batchSize, total)))
        .collect(toList());
```

比 `for (int i = 0; i < size; i += batchSize)` 更能表达意图，
`Math.min` 天然处理「最后一批不足」的边界。

**场景**：批量插入避免逐条 SQL 太慢，也避免一次 insert 几万条撑爆参数上限。

### 6.4 重试模板：短路表达「试到成功为止」

```java
return IntStream.rangeClosed(1, maxAttempts)
        .mapToObj(attempt -> { try { return action.get(); } catch (...) { return null; } })
        .filter(Objects::nonNull)
        .findFirst();                    // 短路：成功后不再尝试
```

测试 `retryStopsOnSuccess`：第 3 次成功后，总尝试次数正好 3（不是 5）。

---

## 7. 高级收集器：一次遍历出报表

📁 `collector/CollectorShowcase.java`

**核心认知**：`groupingBy` 的**下游收集器可以嵌套**，这是从「转换集合」跨到「统计报表」的关键。

### 7.1 常用组合速查

| 需求 | 写法 |
| --- | --- |
| 多级分组 | `groupingBy(城市, groupingBy(渠道))` |
| 分组求和（金额） | `groupingBy(城市, reducing(ZERO, Order::amount, BigDecimal::add))` |
| 分组计数 | `groupingBy(城市, counting())` |
| 分组取最大 | `groupingBy(城市, maxBy(comparing(Order::amount)))` |
| 分组 + 摊平 + 去重 | `groupingBy(城市, flatMapping(o -> o.getItems().stream(), mapping(Item::getName, toSet())))` |
| 分组内过滤（保留空组） | `groupingBy(城市, filtering(条件, toList()))` |
| 有序分组 | `groupingBy(城市, TreeMap::new, counting())` |
| 数值多指标 | `summarizingInt(Order::totalQuantity)` |
| 拼文案 | `joining(", ", "[", "]")` |

### 7.2 ⚠️ BigDecimal 没有 summarizing 版本

`summarizingInt/Long/Double` 只支持基本类型，**金额统计必须用 `reducing` 并给单位元 `ZERO`**。
用 double 算钱会丢精度，这是财务场景的红线。

### 7.3 `flatMapping`（Java 9+）的价值

**场景**：城市 → 该城市买过的商品名集合。

```java
// ✅ 一次遍历
collect(groupingBy(Order::getCity,
        flatMapping(o -> o.getItems().stream(),
                mapping(OrderItem::getName, toSet()))));

// ❌ 没有 flatMapping：先分组拿 List<Order>，再对每组单独 stream 一次
```

### 7.4 `filtering` 放下游 vs 先 filter（报表补零的关键）

```java
// ✅ 下游 filtering：某城市没有大额单，键仍存在，值为空列表
collect(groupingBy(Order::getCity, filtering(大额条件, toList())));

// ❌ 先 filter 再 groupingBy：那个城市整个消失
orders.stream().filter(大额条件).collect(groupingBy(Order::getCity));
```

**测试 `downstreamFilteringKeepsAllKeys` 专门验证**：上海只有空明细订单，
用下游 `filtering` 时「上海」键仍在（值为空列表）。

> **报表要求「每个城市都要有一行，没数据补 0」时，必须用下游 filtering。**
> 这个差别很容易踩，且线上表现是「报表少了几行」，不易察觉。

### 7.5 按业务键去重（`distinct()` 不管用）

```java
// ❌ distinct() 依赖 equals/hashCode，订单没重写就按引用比，等于没去重
orders.stream().distinct()

// ✅ 按业务键 toMap，合并函数明确「以谁为准」
orders.stream().collect(toMap(
        Order::getOrderNo, identity(),
        BinaryOperator.maxBy(comparing(Order::getCreatedAt)),   // 保留最新
        LinkedHashMap::new))                                    // 保序
        .values().stream().collect(toList());
```

### 7.6 排序后收集要用 LinkedHashMap

```java
.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
.collect(toMap(key, value, (a,b) -> a, LinkedHashMap::new));   // ← 必须指定
```

用默认 `HashMap` 会把排好的顺序打乱——**排行榜变乱序**是常见 bug。

---

## 8. 流机制与六个真实陷阱

📁 `mechanics/MechanicsShowcase.java`

### 8.1 惰性 + 短路：为什么 filter 要放 map 前面

**流是「一个元素走完整条管道」，不是「先全部 filter 再全部 map」。**

```java
orders.stream()
      .filter(便宜条件)      // 先过滤，减少后续计算量
      .map(昂贵转换)
      .findFirst();          // 短路：找到就停
```

测试 `findFirstShortCircuits`：6 个订单里找第一个大额单，
`peek` 探针只被调用 **2 次**（不是 6 次）——证明后面的元素根本没处理。

**业务价值**：从大量数据里找第一条符合条件的，不需要全量计算。

### 8.2 陷阱一：流是一次性的

```java
Stream<Order> s = orders.stream();
s.count();
s.count();     // ❌ IllegalStateException: stream has already been operated upon or closed
```

**解法**：每次从集合重新造流，或传 `Supplier<Stream<T>>`（传「造流的方法」而非流本身）。
这也是 Supplier 的一个真实用途。

### 8.3 陷阱二：`toMap` 不给合并函数

键重复直接抛异常。**永远显式写合并函数**，哪怕你确信不会重复——
上游数据变化时这行能救命。

### 8.4 陷阱三：排序遇 null

```java
// ❌ 未支付订单 paidAt 为 null → NPE
.sorted(comparing(Order::getPaidAt))

// ✅ 显式处理 null
.sorted(comparing(Order::amount).reversed()
        .thenComparing(Order::getCreatedAt, nullsLast(naturalOrder())))
```

### 8.5 陷阱四：`takeWhile` ≠ `filter`

```java
takeWhile(条件)   // 遇到第一个不满足就【停止】，后面即使有满足的也不要
filter(条件)      // 遍历全部，挑出所有满足的
```

测试 `takeWhileStopsAtFirstMismatch`：按时间排序后首单已支付，
`takeWhile(!isPaid)` 返回**空列表**，而 `filter` 能找到 3 条。

**场景**：取「连续未支付」的前缀做催付，遇到已支付就停。

### 8.6 陷阱五：并行流的误用

```java
// ✅ 安全：reduce 带单位元 + 结合律运算
orders.parallelStream().map(Order::amount).reduce(ZERO, BigDecimal::add);

// ❌ 危险：并行流里往 ArrayList add，线程不安全，随机丢数据
orders.parallelStream().forEach(list::add);
```

**何时该用并行流**：数据量大（万级以上）+ 单元素计算重 + 无共享状态 + 无顺序要求。
**小集合用并行流通常更慢**（线程调度开销 > 收益）。别默认加 `parallel`。

### 8.7 陷阱六：在 map 里产生副作用

见 [3.3](#33--consumer-的边界别在-map-里改对象)。`map` 应返回新值，改原对象用 `forEach`。

---

## 9. 数据库实战：用 Stream 消除 N+1

📁 `repository/OrderQueryService.java`

**这是 Stream 在企业项目里收益最大的应用**，比集合转换重要得多。

### 9.1 N+1 问题长什么样

```java
// ❌ 每条订单查一次会员：1 + N 次 SQL
return orders.stream().map(o -> {
    Member m = memberRepo.findById(o.getMemberId()).orElseThrow();  // N 次查询！
    return toView(o, m);
}).collect(toList());
```

1000 条订单 = 1001 次 SQL。这是接口慢的头号原因之一。

### 9.2 正确做法：2 次 SQL，与数据量无关

```java
// 1. 查订单（1 次 SQL）
List<OrderEntity> orderList = orders.findByCity(city);

// 2. Stream 提取【去重】的 memberId，一次 IN 查会员（1 次 SQL）
Set<Long> memberIds = orderList.stream()
        .map(OrderEntity::getMemberId)
        .collect(Collectors.toSet());        // ← 去重很关键
Map<Long, MemberEntity> index = members.findByIdIn(memberIds).stream()
        .collect(toMap(MemberEntity::getId, identity(), (a,b) -> a));

// 3. 内存关联（0 次 SQL）
return orderList.stream()
        .map(o -> new OrderWithMember(o.getOrderNo(), index.get(o.getMemberId())...))
        .collect(toList());
```

**三个要点**：
1. `map(...).collect(toSet())` 是「批量参数准备」——1000 条订单可能只涉及 50 个会员，去重后 IN 参数少得多
2. `toMap` 建索引，把查找降成 O(1)
3. 关联在内存做，不再碰数据库

测试 `joinsMembersWithBatchQuery` 验证：4 条订单共享 2 个会员，关联结果正确。

### 9.3 增量同步：先建 Set 再 filter

```java
// ❌ 每条都 existsByOrderNo 查一次 → 又是 N+1
// ✅ 一次查出已存在的键，内存 filter
Set<String> existing = orders.findAll().stream()
        .map(OrderEntity::getOrderNo).collect(toSet());
List<OrderEntity> fresh = incoming.stream()
        .filter(o -> !existing.contains(o.getOrderNo())).collect(toList());
orders.saveAll(fresh);
```

### 9.4 分批写入 + 失败隔离

```java
return batchTemplate.process(() -> incoming,
        o -> o.getOrderNo() != null && o.getAmount() != null,
        Function.identity(),
        orders::saveAll,        // 每批一次 saveAll
        batchSize);
```

测试 `importInBatches`：25 条按 10 分批，全部成功。

### 9.5 ⚠️ 但聚合统计该交给 SQL

```java
// 本模块为演示语法这样写：
orders.findAll().stream().collect(groupingBy(城市, reducing(...)));

// 真实项目应该用 SQL：
// SELECT city, SUM(amount) FROM stream_order GROUP BY city
```

**原因**：`findAll()` 把全表拉进内存，数据量大时会 OOM；
数据库的 GROUP BY 有索引和聚合优化，快得多。详见下一节。

---

## 10. 什么时候「不该」用 Stream

**这一节和前面同样重要**。Stream 不是万能锤。

| 场景 | 更好的选择 | 原因 |
| --- | --- | --- |
| 大数据量聚合统计 | **SQL GROUP BY** | 别把全表拉进内存；数据库有索引优化 |
| 需要 break/continue/return 的复杂控制流 | **for 循环** | Stream 里无法优雅中断，硬套反而难读 |
| 需要修改索引、双向遍历 | **for 循环** | Stream 不暴露索引 |
| 受检异常密集（IO/SQL） | **for 循环**或包装装饰器 | lambda 不能抛受检异常，包装后可读性下降 |
| 三五行就能写完的简单遍历 | **for 循环** | 别为了用 Stream 而用 |
| 需要复用中间结果多次 | **先 collect 再用** | 流一次性，重复造流反而浪费 |
| 小集合 + 轻计算 | **串行流** | 并行调度开销大于收益 |
| 调试困难的长管道 | **拆成命名方法** | 一行 20 个算子无法断点，拆开后每步可测 |

### 判断标准（我的经验）

**用 Stream**：数据转换、筛选、分组统计（中小数据量）、行为参数化、消除 if-else。
**用 for**：复杂控制流、受检异常密集、需要索引、性能敏感的热路径。
**用 SQL**：大数据量聚合、分页、排序取 TopN。

### 可读性红线

```java
// ❌ 一行 15 个算子，无法调试、无法复用
list.stream().filter(...).map(...).flatMap(...).filter(...).sorted(...).map(...)...

// ✅ 拆成命名方法，每步可测
List<Order> paid = filterPaid(orders);
Map<String, BigDecimal> byCity = sumByCity(paid);
return topCities(byCity, 10);
```

**管道超过 5~6 个算子就该考虑拆**。优雅 ≠ 一行写完。

---

## 11. 好处总览与场景速查表

### 11.1 好处（按收益排序）

| # | 好处 | 具体体现 | 代码位置 |
| --- | --- | --- | --- |
| 1 | **消除 N+1，性能数量级提升** | 1001 次 SQL → 2 次 | `OrderQueryService.listWithMemberLevel` |
| 2 | **省掉不必要的 IO/计算** | Supplier 短路，缓存命中不查库 | `SupplierShowcase.firstNonEmpty` |
| 3 | **符合开闭原则** | 新增策略/条件不改原方法 | `DiscountStrategy`、`PredicateShowcase.filter` |
| 4 | **骨架复用，业务填空** | 批处理模板写一次，N 个导入复用 | `BatchTemplate.process` |
| 5 | **一次遍历出多指标** | 分组+聚合+排序一趟完成 | `CollectorShowcase` |
| 6 | **失败隔离** | 单条脏数据不中断整批 | `ConsumerShowcase.forEachSafely` |
| 7 | **可测试性** | 每个 lambda 可独立单测 | 全部 44 个测试 |
| 8 | **意图清晰** | 声明式表达「做什么」 | `filterPaid` 比 if 嵌套易读 |
| 9 | **不可变友好** | 转换生成新对象，不改原数据 | `FunctionShowcase.toViews` |
| 10 | **并行能力就绪** | 结合律运算可安全并行 | `MechanicsShowcase.parallelTotal` |

### 11.2 场景 → 手法速查

| 业务场景 | 用什么 | 关键 API |
| --- | --- | --- |
| 多级缓存兜底 | Supplier 链 | `Stream.of(suppliers).map(get).findFirst` |
| 昂贵值只算一次 | 记忆化 | `computeIfAbsent` |
| 取默认值别触发查询 | 延迟兜底 | `orElseGet` 而非 `orElse` |
| 批量造号/造数据 | 无限流 | `generate` + `limit` |
| 输出前多步加工 | Consumer 链 | `reduce(空操作, Consumer::andThen)` |
| 批处理容错 | 装饰器 | try-catch 包 `accept` |
| 动态查询条件 | Predicate 组合 | `reduce(恒真, Predicate::and)` |
| 风控任一命中 | Predicate 或 | `reduce(恒假, Predicate::or)` |
| 成功/失败分流 | 分区 | `partitioningBy` |
| 两集合关联 | 建索引 | `toMap` + 合并函数 |
| 对象转 DTO | Function 组合 | `andThen` |
| 多种折扣/费率/规则 | 枚举策略 | 枚举 + `Function` 字段 |
| 通用导入/同步 | 回调模板 | 四大接口做参数 |
| 分批落库 | 索引流 | `IntStream.iterate` 三参 |
| 调三方重试 | 短路 | `rangeClosed` + `findFirst` |
| 多级报表 | 嵌套分组 | `groupingBy(f, groupingBy(g))` |
| 分组内摊平去重 | 下游摊平 | `flatMapping` + `mapping` + `toSet` |
| 报表补零 | 下游过滤 | `filtering`（保留空组） |
| 排行榜 | 有序收集 | `toMap(..., LinkedHashMap::new)` |
| 按业务键去重 | 合并函数 | `toMap` + `maxBy` |
| 找第一个符合的 | 短路 | `findFirst`（filter 前置） |
| 取连续前缀 | 截断 | `takeWhile` |
| 嵌套集合摊平 | 摊平 | `flatMap`（可带父级字段） |
| 消除 N+1 | 批量+索引 | `map→toSet` → `findByIdIn` → `toMap` |
| 增量同步 | 建 Set 过滤 | `collect(toSet)` + `filter` |

---

## 12. 学习路径与练习建议

### 12.1 建议顺序（每步都有测试可对照）

1. **先跑测试看行为**
   ```
   mvn -pl allst-springboot-stream test
   ```
   44 个测试的断言就是「预期行为说明书」。

2. **读 `FunctionalInterfaceTest`**，对照 `functional/` 四个类。
   重点看 `supplierShortCircuitsLaterSources`——理解「省 IO」而非「少写代码」。

3. **改造你自己的一个方法**：找一个有 if-else 链或多次循环的方法，
   用 Predicate 组合或枚举策略重写，跑测试确认行为不变。

4. **读 `OrderQueryService`**，找你项目里的 N+1（日志里 SQL 条数异常多的接口），
   用「批量查 + 建索引」改造。**这一步收益最大。**

5. **读 `MechanicsAndCallbackTest`**，特别是六个陷阱的测试。
   这些是线上事故的常见来源。

6. **最后读 `CollectorShowcase`**，报表类需求直接查速查表。

### 12.2 三个练习题（建议动手）

**练习 1（易）**：给 `PredicateShowcase` 加一个「近 7 天创建」的条件，
并写测试验证它与 `inCity` 组合后的结果。
> 提示：用 `createdBetween`，注意边界含不含。

**练习 2（中）**：给 `DiscountStrategy` 加一种「阶梯满减」券
（满 300 减 30，满 600 减 80），要求**不改动 `DiscountService` 任何一行**。
> 检验点：如果你需要改 `DiscountService`，说明策略抽象没做对。

**练习 3（难）**：把 `OrderQueryService.listWithMemberLevel` 改成
「支持按会员等级过滤 + 分页」，要求 SQL 次数仍为常数级。
> 提示：先想清楚过滤该在 SQL 还是内存做——这涉及第 10 节的取舍。

### 12.3 开源参考方向

想看更大规模的真实用法，建议按这些关键词找源码（比看教程有效）：

- **Spring Framework 自身**：搜 `@FunctionalInterface`，看 `Function`/`Supplier` 如何做扩展点
  （本模块 security 的 `TokenCustomizer`、`LoginGuard` 就是同一手法）
- **MyBatis / Netty**：大量 `Consumer` 回调与建造者链
- **Guava**：`Streams` 工具类（`zip`、`findLast` 等 JDK 缺失的操作）
- **StreamEx**（amaembo/streamex）：JDK Stream 的增强库，看它补了哪些能力，
  能反向理解 JDK Stream 的边界

> 我没有在本模块引入这些第三方库——**先用透 JDK 原生能力**，
> 遇到确实缺失的操作再考虑引库，避免过早依赖。

---

## 附录：代码地图

```
allst-springboot-stream/
├── STREAM-GUIDE.md                    ← 本文
├── src/main/java/com/allst/boot/
│   ├── domain/                        领域模型（订单/明细/会员/视图/枚举）
│   │   ├── Order.java                 金额等计算收敛在此，让管道可读
│   │   ├── OrderItem.java             一对多，供 flatMap 演示
│   │   ├── OrderStatus.java           枚举内用 Stream 算终态集合
│   │   └── Channel.java               业务属性收进枚举
│   ├── support/Orders.java            固定演示数据（含空明细等边界）
│   ├── functional/                    第 1 层：四大接口
│   │   ├── SupplierShowcase.java      兜底链、记忆化、generate、orElseGet
│   │   ├── ConsumerShowcase.java      andThen 加工、失败隔离
│   │   ├── FunctionShowcase.java      组合、toMap 建索引
│   │   └── PredicateShowcase.java     and/or/not、partitioningBy
│   ├── strategy/                      第 2 层：函数式策略
│   │   ├── DiscountStrategy.java      枚举 + Function
│   │   └── DiscountService.java       策略 + 分组聚合编排
│   ├── callback/BatchTemplate.java    第 3 层：回调模板、分批、重试
│   ├── collector/CollectorShowcase.java  第 4 层：高级收集器
│   ├── mechanics/MechanicsShowcase.java  第 5 层：机制与陷阱
│   ├── repository/                    第 6 层：数据库实战
│   │   ├── OrderEntity.java           刻意不建 JPA 关联
│   │   ├── MemberEntity.java
│   │   ├── OrderRepository.java
│   │   ├── MemberRepository.java      findByIdIn 批量查
│   │   └── OrderQueryService.java     ★ 消除 N+1
│   └── web/StreamDemoController.java  演示接口（控制器极薄）
└── src/test/java/com/allst/boot/
    ├── functional/FunctionalInterfaceTest.java      14 个
    ├── strategy/StrategyAndCollectorTest.java       12 个
    ├── mechanics/MechanicsAndCallbackTest.java      13 个
    └── repository/OrderQueryServiceTest.java         5 个
```

### 演示接口

| 接口 | 演示内容 |
| --- | --- |
| `GET /api/stream/filter?city=成都&minAmount=300` | Predicate 可组合筛选 |
| `GET /api/stream/views` | Function 组合 + 索引关联 |
| `GET /api/stream/revenue-by-city` | groupingBy + reducing |
| `GET /api/stream/products-by-city` | flatMapping |
| `GET /api/stream/coupon-savings` | 策略 + 分组统计 |
| `GET /api/stream/items` | flatMap 带父级字段 |

---

*本文所有代码均可运行，44 个测试全绿。行号随改动变化，请以类名/方法名回溯。*