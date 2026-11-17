package com.allst.springboot.behavioral.mediator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 中介者（Mediator）——支付成功后，库存、积分、通知、审计之间不互相引用。
 * <Pre>
 * 场景：如果订单服务直接调库存、库存再调积分、积分再调通知，几个服务会织成一张网，
 * 谁先谁后、失败是否继续都散落在各处。中介者集中编排「支付成功」这条协作流程，
 * 每个同事只实现自己那一步，彼此不知道对方存在。
 * <Pre>
 * 与观察者的区别：中介者认识所有同事、主动决定顺序和是否中断；
 * 观察者里发布者完全不知道有哪些订阅者（见 behavioral.observer）。
 * 需要「顺序和条件」时用中介者，需要「随便挂多少个、彼此无关」时用事件。
 */
@Component
public class OrderMediator {

    public interface Colleague {
        String name();

        String onPaid(String orderNo, BigDecimal amount);
    }

    private final List<Colleague> colleagues;

    public OrderMediator() {
        this.colleagues = Arrays.asList(
                new InventoryColleague(),
                new PointsColleague(),
                new NoticeColleague(),
                new AuditColleague());
    }

    public List<String> paid(String orderNo, BigDecimal amount) {
        List<String> steps = new ArrayList<>();
        for (Colleague colleague : colleagues) {
            steps.add(colleague.name() + ": " + colleague.onPaid(orderNo, amount));
        }
        return Collections.unmodifiableList(steps);
    }

    public List<String> colleagueNames() {
        List<String> names = new ArrayList<>();
        colleagues.forEach(c -> names.add(c.name()));
        return names;
    }

    private static final class InventoryColleague implements Colleague {
        @Override
        public String name() {
            return "库存";
        }

        @Override
        public String onPaid(String orderNo, BigDecimal amount) {
            return "占用库存转实扣，订单 " + orderNo;
        }
    }

    private static final class PointsColleague implements Colleague {
        @Override
        public String name() {
            return "积分";
        }

        @Override
        public String onPaid(String orderNo, BigDecimal amount) {
            return "发放 " + amount.intValue() + " 积分";
        }
    }

    private static final class NoticeColleague implements Colleague {
        @Override
        public String name() {
            return "通知";
        }

        @Override
        public String onPaid(String orderNo, BigDecimal amount) {
            return "推送支付成功提醒";
        }
    }

    private static final class AuditColleague implements Colleague {
        @Override
        public String name() {
            return "审计";
        }

        @Override
        public String onPaid(String orderNo, BigDecimal amount) {
            return "写入支付流水 " + amount.toPlainString();
        }
    }
}
