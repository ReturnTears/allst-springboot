# WebFlux
```text
ServerResponse 是对响应的封装，可以设置响应状态、响应头、响应正文。比如 ok 代表的是 200 响应码、MediaType 枚举是代表这文本内容类型、返回的是 String 的对象。

RouterFunctions.route(RequestPredicate, HandlerFunction) 方法，对应的入参是请求参数和处理函数，如果请求匹配，就调用对应的处理器函数。

Mono 和 Flux 适用于两个场景，即：
Mono：实现发布者，并返回 0 或 1 个元素，即单对象。
Flux：实现发布者，并返回 N 个元素，即 List 列表对象。

Mono 常用的方法有：
Mono.create()：使用 MonoSink 来创建 Mono。
Mono.justOrEmpty()：从一个 Optional 对象或 null 对象中创建 Mono。
Mono.error()：创建一个只包含错误消息的 Mono。
Mono.never()：创建一个不包含任何消息通知的 Mono。
Mono.delay()：在指定的延迟时间之后，创建一个 Mono，产生数字 0 作为唯一值。


Flux 是响应流 Publisher 具有基础 rx 操作符，可以成功发布 0 到 N 个元素或者错误。Flux 其实是 Mono 的一个补充
所以要注意：如果知道 Publisher 是 0 或 1 个，则用 Mono。
Flux 最值得一提的是 fromIterable 方法，fromIterable(Iterable it) 可以发布 Iterable 类型的元素。当然，Flux 也包含了基础的操作：map、merge、concat、flatMap、take

```

## REST
```text
REST 是属于 Web 自身的一种架构风格，是在 HTTP 1.1 规范下实现的。Representational State Transfer 全称翻译为表现层状态转化。Resource：资源。
比如 newsfeed；Representational：表现形式，比如用 JSON、富文本等；State Transfer：状态变化。通过 HTTP 动作实现。

理解 REST，要明白五个关键要素：

资源（Resource）
资源的表述（Representation）
状态转移（State Transfer）
统一接口（Uniform Interface）
超文本驱动（Hypertext Driven）
6 个主要特性：

面向资源（Resource Oriented）
可寻址（Addressability）
连通性（Connectedness）
无状态（Statelessness）
统一接口（Uniform Interface）
超文本驱动（Hypertext Driven）

```

## Reactive
```text
Spring Data Reactive Repositories 目前支持 Mongo、Cassandra、Redis、Couchbase。不支持 MySQL，

Spring Data Reactive Repositories 突出点是 Reactive，即非阻塞的。区别如下：

基于 JDBC 实现的 Spring Data，比如 Spring Data JPA 是阻塞的。原理是基于阻塞 IO 模型 消耗每个调用数据库的线程（Connection）。
事务只能在一个 java.sql.Connection 使用，即一个事务一个操作。

那如何异步非阻塞封装下 JDBC 的思想也不新鲜，Scala 库 Slick 3 就实现了。简单的实现原理如下：
1、一个事务多个操作，那么共享一个 java.sql.Connection，可以使用透明事务管理，利用回调编程模型去传递。
2、保持有限的空闲连接。

```

# Error
```text
1、启动SpringBoot项目java: You aren't using a compiler supported by lombok, so lombok will not work and has been disabled.
  Your processor is: com.sun.proxy.$Proxy17
  Lombok supports: sun/apple javac 1.6, ECJ
无奈之举：
讲@Data注解还原为 Getter Setter 方法  


2、连接MySQL报错：ERROR 2026 (HY000): SSL connection error: unknown error number
方法 1：禁用 SSL（仅用于测试）
mysql --ssl-mode=DISABLED -h hostname -u username -p
```