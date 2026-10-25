# 简化前：一条履约流程里叠了四类复杂度

业务是跨境订单履约。一笔订单要经过支付、风控、占库存、发货、签收，中途还可能取消、超时关闭、缺货、未发货退款、签收后的退货审核。这些分支如果写在一个服务里，方法会变成下面这种形状。

```java
public void onPaySuccess(Order order) {
    if (!"PAYING".equals(order.getStatus())) {
        throw new IllegalStateException("状态不对");
    }
    order.setStatus("PAID");
    order.setPaidAt(now());
    if (order.getRiskScore() >= 80) {
        order.setStatus("RISK_REVIEW");
        return;
    }
    order.setStatus("WAIT_ALLOCATE");
}

public void allocate(Order order) {
    if (!"WAIT_ALLOCATE".equals(order.getStatus()) && !"STOCK_SHORT".equals(order.getStatus())) {
        throw new IllegalStateException("现在不能占库存");
    }
    if (stock.getAvailable() >= order.getQuantity()) {
        stock.setAvailable(stock.getAvailable() - order.getQuantity());
        order.setStatus("WAIT_SHIP");
        notifyWarehouse(order);
    } else {
        order.setStatus("STOCK_SHORT");
    }
}
```

支付、发货、退货、超时关闭各自再写一份类似的判断。问题不在某一段 if，而在四类事情缠在一起：

1. **转移规则。** “待支付可以发起支付，待发货才能发货”散落在每个方法开头。新增一个状态时，要翻遍所有方法，看谁漏了判断。
2. **分支策略。** 支付成功后按风险分分流，占库存时按余量分流。策略写在流程方法中间，换阈值就要改流程。
3. **副作用。** 记支付时间、扣库存、通知仓库、通知退款和改状态写在同一次调用里。通知失败时，很难分清状态该不该回滚。
4. **并发。** 同一订单的支付回调和取消可能同时进来；两笔订单可能同时抢最后一件库存。过程式代码没有统一的互斥点。

页面也会复制一份规则：前端自己用 `if (status == 'WAIT_SHIP') 显示发货按钮`。后端改一条转移，按钮可能还是旧的。

本模块要达到的结果是：

- 转移规则集中在 `TradeOrderStateMachineConfig`。
- 能否通过集中在守卫，通过之后做什么集中在动作。
- 订单状态字段只由 `TradeOrderFlowService` 按机器结果回写。
- 页面按钮来自 `OrderEventCatalog`，和机器使用同一份状态语义。
- 外部通知在事务提交之后执行，库存扣减和状态变更留在同一个事务里。

合同和发票是补充，用来说明另外两种形状：两条审核线同时进行，以及一条很扁的审批链。它们的文档在 [06-contract-regions.md](06-contract-regions.md) 和 [07-invoice-cola.md](07-invoice-cola.md)。
