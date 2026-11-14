package com.allst.springboot.behavioral.observer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 观察者（Observer）——订单支付成功后要发积分、发短信、写审计、通知仓库。
 *
 * 场景：如果都写在下单方法里，每加一个后续动作就要改一次主流程，而且
 * 「通知仓库」这类外部调用不该拖住或回滚主事务。Spring 的事件机制让发布者
 * 只 publish 一次，订阅者各自挂上，互不知道对方存在。
 *
 * 两种时机要分清：
 *  - @EventListener：同步执行、和发布者在同一事务里，适合积分/审计这类要一起回滚的；
 *  - @TransactionalEventListener(AFTER_COMMIT)：事务提交后才执行，适合短信/仓库通知，
 *    否则事务回滚了消息已经发出去。没有事务上下文时它默认不触发。
 *
 * 再挂一个订阅者不用改 PlaceOrderFacade；要异步再加 @Async（本模块没开异步，
 * 避免示例引入线程池和事务边界的额外变量）。
 */
@Component
public class OrderEventListeners {

    private final List<String> synchronousTrail = new CopyOnWriteArrayList<>();
    private final List<String> afterCommitTrail = new CopyOnWriteArrayList<>();

    @EventListener
    public void grantPoints(OrderPlacedEvent event) {
        synchronousTrail.add("积分: " + event.getBuyer() + " +" + event.getAmount().intValue());
    }

    @EventListener
    public void writeAudit(OrderPlacedEvent event) {
        synchronousTrail.add("审计: " + event.getOrderNo() + " 支付 " + event.getAmount().toPlainString());
    }

    @EventListener
    public void sendReceipt(OrderPlacedEvent event) {
        synchronousTrail.add("回执: " + event.getOrderNo() + " 渠道 " + event.getChannel());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyWarehouse(OrderPlacedEvent event) {
        afterCommitTrail.add("提交后通知仓库: " + event.getOrderNo());
    }

    public Map<String, Object> trail() {
        Map<String, Object> trail = new LinkedHashMap<>();
        trail.put("inTransaction", List.copyOf(synchronousTrail));
        trail.put("afterCommit", List.copyOf(afterCommitTrail));
        return trail;
    }
}
