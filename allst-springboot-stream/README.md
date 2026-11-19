# 答疑解惑

## 疑问点1

com.allst.boot.functional.PredicateShowcase
```java
public Predicate<Order> amountAtLeast(BigDecimal threshold) {
    return order -> order.amount().compareTo(threshold) >= 0;
}
// 等价于
public Predicate<Order> amountAtLeast(BigDecimal threshold) {
    return new Predicate<Order>() {
        @Override
        public boolean test(Order order) {        // ← order 就是 test 方法的形参
            return order.amount().compareTo(threshold) >= 0;
        }
    };
}
```

com.allst.boot.functional.FunctionalInterfaceTest#forEachSafelyIsolatesFailures
```java
int failures = consumers.forEachSafely(items, item -> {
    if ("bad".equals(item)) {
        throw new IllegalStateException("脏数据");
    }
    processed.add(item);
});
// forEachSafely方法第2参数等价于：
Consumer<String> action = new Consumer<String>() {
    @Override
    public void accept(String item) {      // ← item 是 accept 的形参
        if ("bad".equals(item)) {
            throw new IllegalStateException("脏数据");
        }
        processed.add(item);
    }
};
int failures = consumers.forEachSafely(items, action);
// Consumer<T> 接口只有一个方法 void accept(T t)——接收一个值，做点事，不返回结果。所以 action 就是"给我一个元素，我告诉你拿它干什么"。
```

