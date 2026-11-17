package com.allst.springboot.behavioral.observer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 观察者（Observer）里的「事件」。Spring 4.2 之后事件不必继承 ApplicationEvent，
 * 一个普通 POJO 就可以，发布者只认这个类型。
 */
@Getter
@AllArgsConstructor
public class OrderPlacedEvent {
    private final String orderNo;
    private final String buyer;
    private final BigDecimal amount;
    private final String channel;
}
