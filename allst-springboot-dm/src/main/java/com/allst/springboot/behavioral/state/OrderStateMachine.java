package com.allst.springboot.behavioral.state;

import com.allst.springboot.common.BizException;
import com.allst.springboot.domain.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 状态（State）——把「什么状态下允许什么事件」收在一张转移表里。
 *
 * 场景：订单有 已创建/已支付/已发货/已关闭/已取消，支付回调、发货、取消、超时关闭
 * 都可能并发到达。把判断写成每个方法开头的 if (status == ...)，新增状态要翻遍所有方法。
 * 转移表让非法事件在进入业务动作之前就被拒绝。
 *
 * 这是简版「查表式状态模式」：状态是枚举，行为差异由调用方按新状态选择。
 * 完整的、带守卫/动作/持久化的状态机见同仓库的 allst-springboot-state-machine 模块。
 */
@Component
public class OrderStateMachine {

    public enum Event {
        PAY, SHIP, CANCEL, CLOSE
    }

    private final Map<OrderStatus, Map<Event, OrderStatus>> transitions =
            new EnumMap<>(OrderStatus.class);

    public OrderStateMachine() {
        define(OrderStatus.CREATED, Event.PAY, OrderStatus.PAID);
        define(OrderStatus.CREATED, Event.CANCEL, OrderStatus.CANCELLED);
        define(OrderStatus.CREATED, Event.CLOSE, OrderStatus.CLOSED);
        define(OrderStatus.PAID, Event.SHIP, OrderStatus.SHIPPED);
        define(OrderStatus.PAID, Event.CANCEL, OrderStatus.CANCELLED);
    }

    public OrderStatus fire(OrderStatus current, Event event) {
        Map<Event, OrderStatus> allowed = transitions.getOrDefault(current, new EnumMap<>(Event.class));
        OrderStatus next = allowed.get(event);
        if (next == null) {
            throw new BizException("状态 " + current + " 不允许事件 " + event);
        }
        return next;
    }

    public List<String> allowed(OrderStatus current) {
        List<String> events = new ArrayList<>();
        transitions.getOrDefault(current, new EnumMap<>(Event.class))
                .keySet().forEach(e -> events.add(e.name()));
        return events;
    }

    public Map<String, String> transitionTable() {
        Map<String, String> table = new LinkedHashMap<>();
        transitions.forEach((state, events) ->
                events.forEach((event, target) ->
                        table.put(state + "--" + event, target.name())));
        return table;
    }

    private void define(OrderStatus from, Event event, OrderStatus to) {
        transitions.computeIfAbsent(from, k -> new EnumMap<>(Event.class)).put(event, to);
    }
}
