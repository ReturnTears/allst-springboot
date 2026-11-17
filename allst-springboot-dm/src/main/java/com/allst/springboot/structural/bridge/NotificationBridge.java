package com.allst.springboot.structural.bridge;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 桥接（Bridge）——消息「种类」和「发送通道」两个维度各自变化。
 * <Pre>
 * 场景：订单通知、营销通知、系统告警，要能发短信、邮件、App 推送。
 * 如果用继承，3 种消息 × 3 种通道 = 9 个类；加一种通道要改 3 个分支。
 * 桥接把「消息要说什么」（抽象）和「怎么送出去」（实现）拆开：
 * 抽象层只持有 Sender 引用，两个维度各自扩展、运行时组合。
 * <Pre>
 * 真实 Spring 项目里两个维度通常都是 bean，由注入的 Map 选择；这里收在一个类里
 * 是为了一眼看清桥的两端。JDBC 驱动、SLF4J 绑定都是同一种思路。
 */
@Component
public class NotificationBridge {

    private final List<String> sent = new CopyOnWriteArrayList<>();

    /** 实现化角色：只管把一段文本送到某个通道 */
    public interface Sender {
        String channel();

        String send(String target, String content);
    }

    /** 抽象化角色：只管内容，通道交给组合进来的 Sender */
    public abstract static class Notification {
        protected final Sender sender;

        protected Notification(Sender sender) {
            this.sender = sender;
        }

        public final String deliver(String target) {
            return sender.send(target, render());
        }

        protected abstract String render();
    }

    public static final class OrderNotification extends Notification {
        private final String orderNo;

        public OrderNotification(Sender sender, String orderNo) {
            super(sender);
            this.orderNo = orderNo;
        }

        @Override
        protected String render() {
            return "订单 " + orderNo + " 已支付";
        }
    }

    public static final class AlertNotification extends Notification {
        private final String reason;

        public AlertNotification(Sender sender, String reason) {
            super(sender);
            this.reason = reason;
        }

        @Override
        protected String render() {
            return "[告警] " + reason;
        }
    }

    public static final class SmsSender implements Sender {
        @Override
        public String channel() {
            return "SMS";
        }

        @Override
        public String send(String target, String content) {
            return "SMS->" + target + ":" + content;
        }
    }

    public static final class EmailSender implements Sender {
        @Override
        public String channel() {
            return "EMAIL";
        }

        @Override
        public String send(String target, String content) {
            return "EMAIL->" + target + ":" + content;
        }
    }

    public static final class AppPushSender implements Sender {
        @Override
        public String channel() {
            return "APP_PUSH";
        }

        @Override
        public String send(String target, String content) {
            return "PUSH->" + target + ":" + content;
        }
    }

    public String send(String kind, String channel, String target, String payload) {
        Sender sender = sender(channel);
        Notification notification = "ALERT".equalsIgnoreCase(kind)
                ? new AlertNotification(sender, payload)
                : new OrderNotification(sender, payload);
        String receipt = notification.deliver(target);
        sent.add(receipt);
        return receipt;
    }

    public List<String> sent() {
        return List.copyOf(sent);
    }

    private Sender sender(String channel) {
        switch (channel == null ? "" : channel.toUpperCase(Locale.ROOT)) {
            case "EMAIL":
                return new EmailSender();
            case "APP_PUSH":
                return new AppPushSender();
            case "SMS":
                return new SmsSender();
            default:
                throw new IllegalArgumentException("未知通道: " + channel);
        }
    }
}
