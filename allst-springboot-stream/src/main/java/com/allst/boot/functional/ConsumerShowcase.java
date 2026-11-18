package com.allst.boot.functional;

import com.allst.boot.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Consumer（消费者）的业务价值：<b>把「对每个元素做什么」参数化，让遍历骨架只写一次</b>。
 * <p>
 * 三个真实场景：
 * <ol>
 *   <li>{@link #enrich} 用 andThen 把多个加工步骤串成流水线（脱敏 → 补字段 → 打标记）</li>
 *   <li>{@link #forEachSafely} 批处理单条失败不中断整批——这是 Consumer 最实用的装饰器</li>
 *   <li>{@link #pipeline} 把「校验、落库、发消息」等副作用编排成可复用管道</li>
 * </ol>
 * <p>
 * 注意：Consumer 天生用于副作用。在 {@code map} 里改对象是反模式，
 * 要改就用 {@code peek}（调试）或 {@code forEach}（明确的副作用终点）。
 */
@Slf4j
@Component
public class ConsumerShowcase {

    /**
     * 订单加工流水线：多个 Consumer 用 andThen 串起来，一次遍历完成全部加工。
     * <p>
     * 对比传统写法：三个 for 循环各遍历一次，或者一个循环里塞三段逻辑。
     * andThen 的好处是<b>每个步骤可独立测试、可按需组合</b>，调用方决定装哪几步。
     */
    public List<Order> enrich(List<Order> orders, List<Consumer<Order>> steps) {
        Consumer<Order> combined = steps.stream()
                .reduce(order -> { }, Consumer::andThen);
        orders.forEach(combined);
        return orders;
    }

    /** 会员名脱敏：张三 → 张*，欧阳修 → 欧**。 */
    public Consumer<Order> maskMemberName() {
        return order -> {
            String name = order.getMemberName();
            if (name != null && name.length() > 1) {
                order.setMemberName(name.charAt(0) + "*".repeat(name.length() - 1));
            }
        };
    }

    /** 抹掉券码，避免对外泄露营销信息。 */
    public Consumer<Order> clearCoupon() {
        return order -> order.setCouponCode(null);
    }

    /**
     * 安全遍历：单条抛异常时记日志并继续，不中断整批。
     * <p>
     * 这是批处理/数据同步的刚需。{@code Consumer.accept} 不声明受检异常，
     * 所以必须在装饰器里兜住——否则一条脏数据会让整批任务失败。
     *
     * @return 失败条数，便于上层决定是否告警
     */
    public <T> int forEachSafely(List<T> items, Consumer<T> action) {
        int[] failures = {0};
        items.forEach(item -> {
            try {
                action.accept(item);
            } catch (RuntimeException ex) {
                failures[0]++;
                log.warn("单条处理失败，已跳过: type={}, reason={}", item.getClass().getSimpleName(), ex.getMessage());
            }
        });
        return failures[0];
    }

    /**
     * 副作用管道：把多个动作按顺序作用于每个元素，并收集成功项。
     * <p>
     * 业务场景：批量导入订单，每条要「校验 → 落库 → 发通知」。
     * 用 List&lt;Consumer&gt; 编排后，新增一步只是往列表里加一个 lambda，不改遍历代码（开闭原则）。
     */
    public List<Order> pipeline(List<Order> orders, List<Consumer<Order>> actions) {
        List<Order> succeeded = new ArrayList<>();
        Consumer<Order> all = actions.stream().reduce(order -> { }, Consumer::andThen);
        forEachSafely(orders, all.andThen(succeeded::add));
        return succeeded;
    }
}