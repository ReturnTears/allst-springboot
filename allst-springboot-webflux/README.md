# WebFlux
```text
ServerResponse 是对响应的封装，可以设置响应状态、响应头、响应正文。比如 ok 代表的是 200 响应码、MediaType 枚举是代表这文本内容类型、返回的是 String 的对象。

RouterFunctions.route(RequestPredicate, HandlerFunction) 方法，对应的入参是请求参数和处理函数，如果请求匹配，就调用对应的处理器函数。

Mono 和 Flux 适用于两个场景，即：
Mono：实现发布者，并返回 0 或 1 个元素，即单对象。
Flux：实现发布者，并返回 N 个元素，即 List 列表对象。


```