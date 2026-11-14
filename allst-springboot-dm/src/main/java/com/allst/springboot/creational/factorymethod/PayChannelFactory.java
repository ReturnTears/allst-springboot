package com.allst.springboot.creational.factorymethod;

import com.allst.springboot.common.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 工厂方法（Factory Method）——按支付渠道创建「支付命令」。
 *
 * 场景：支付宝、微信、余额各有自己的签名、流水号规则和限额。把「创建哪种命令」
 * 下放给各自的 Creator，支付的主流程（取流水号、执行、拼回执）只写一份。
 *
 * 注意这里同时出现了模板方法：PayCreator.pay() 固定骨架，createCommand() 是工厂方法。
 * 新接一个渠道时，新增一个 Creator + 一个 Command，不用改 pay() 和调用方。
 *
 * Spring 项目里更常见的写法是把每个 Creator/Handler 做成 @Component，再用
 * Map&lt;String, Handler&gt; 注入（见 behavioral.strategy.DiscountStrategyRouter）。
 * 这里保留教科书结构，是为了能看清「创建」这一步被推迟到了子类。
 */
@Component
public class PayChannelFactory {

    private final Map<String, PayCreator> creators = new LinkedHashMap<>();

    public PayChannelFactory() {
        creators.put("ALIPAY", new AlipayCreator());
        creators.put("WECHAT", new WechatCreator());
        creators.put("BALANCE", new BalanceCreator());
    }

    public Set<String> supportedChannels() {
        return Collections.unmodifiableSet(creators.keySet());
    }

    public PayReceipt pay(String channel, String orderNo, BigDecimal amount) {
        String key = channel == null ? "" : channel.toUpperCase(Locale.ROOT);
        PayCreator creator = creators.get(key);
        if (creator == null) {
            throw new BizException("不支持的支付方式: " + channel);
        }
        return creator.pay(orderNo, amount);
    }

    @Getter
    @AllArgsConstructor
    public static final class PayReceipt {
        private final String channel;
        private final String orderNo;
        private final BigDecimal amount;
        private final String serialNo;
        private final String result;
    }

    private interface PayCommand {
        String channel();

        String serialNo();

        String execute();
    }

    /** Creator：声明工厂方法，并固定使用命令的骨架 */
    private abstract static class PayCreator {
        final PayReceipt pay(String orderNo, BigDecimal amount) {
            PayCommand command = createCommand(orderNo, amount);
            String result = command.execute();
            return new PayReceipt(command.channel(), orderNo, amount, command.serialNo(), result);
        }

        protected abstract PayCommand createCommand(String orderNo, BigDecimal amount);
    }

    private static final class AlipayCreator extends PayCreator {
        @Override
        protected PayCommand createCommand(String orderNo, BigDecimal amount) {
            return new ThirdPartyCommand("ALIPAY", "ALI" + UUID.randomUUID().toString().substring(0, 12), amount);
        }
    }

    private static final class WechatCreator extends PayCreator {
        @Override
        protected PayCommand createCommand(String orderNo, BigDecimal amount) {
            return new ThirdPartyCommand("WECHAT", "WX" + UUID.randomUUID().toString().substring(0, 12), amount);
        }
    }

    private static final class BalanceCreator extends PayCreator {
        @Override
        protected PayCommand createCommand(String orderNo, BigDecimal amount) {
            return new BalanceCommand(orderNo, amount);
        }
    }

    @AllArgsConstructor
    private static final class ThirdPartyCommand implements PayCommand {
        private final String channel;
        private final String serialNo;
        private final BigDecimal amount;

        @Override
        public String channel() {
            return channel;
        }

        @Override
        public String serialNo() {
            return serialNo;
        }

        @Override
        public String execute() {
            return "调用" + channel + "预下单，金额 " + amount.toPlainString();
        }
    }

    @AllArgsConstructor
    private static final class BalanceCommand implements PayCommand {
        private final String orderNo;
        private final BigDecimal amount;

        @Override
        public String channel() {
            return "BALANCE";
        }

        @Override
        public String serialNo() {
            return "BAL-" + orderNo;
        }

        @Override
        public String execute() {
            return "扣减账户余额 " + amount.toPlainString();
        }
    }
}
