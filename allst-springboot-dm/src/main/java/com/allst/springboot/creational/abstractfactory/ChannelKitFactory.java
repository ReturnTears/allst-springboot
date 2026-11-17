package com.allst.springboot.creational.abstractfactory;

import com.allst.springboot.common.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 抽象工厂（Abstract Factory）——国内站 / 跨境站各用一整套相互匹配的组件。
 * <Pre>
 * 场景：跨境单要用带税结算、报关合规说明和独立号段；国内单用普通结算和增值税说明。
 * 如果分开按「订单号生成器」「结算器」「合规说明」各自 if-else 选择，很容易拼出
 * 「跨境单 + 国内结算」这种不合法组合。抽象工厂一次给一族，保证配套。
 * </Pre>
 * 与工厂方法的区别：工厂方法只创建一个产品，抽象工厂创建一族产品。
 */
@Component
public class ChannelKitFactory {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    public ChannelKit forChannel(String channel) {
        if ("CROSS_BORDER".equalsIgnoreCase(channel)) {
            return new CrossBorderKit();
        }
        if ("DOMESTIC".equalsIgnoreCase(channel)) {
            return new DomesticKit();
        }
        throw new BizException("未知业务渠道: " + channel);
    }

    /** 一族产品：订单号、金额计算、合规说明必须来自同一个渠道 */
    public interface ChannelKit {
        String newOrderNo();

        BigDecimal settle(BigDecimal rawAmount);

        String complianceNote();
    }

    private static final class DomesticKit implements ChannelKit {
        @Override
        public String newOrderNo() {
            return "DM" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + String.format("%06d", SEQUENCE.incrementAndGet());
        }

        @Override
        public BigDecimal settle(BigDecimal rawAmount) {
            return rawAmount.setScale(2, RoundingMode.HALF_UP);
        }

        @Override
        public String complianceNote() {
            return "国内订单：开具增值税发票";
        }
    }

    private static final class CrossBorderKit implements ChannelKit {
        @Override
        public String newOrderNo() {
            return "CB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + String.format("%06d", SEQUENCE.incrementAndGet());
        }

        @Override
        public BigDecimal settle(BigDecimal rawAmount) {
            // 演示用：跨境价含 9% 综合税，直接算进应付金额
            return rawAmount.multiply(new BigDecimal("1.09")).setScale(2, RoundingMode.HALF_UP);
        }

        @Override
        public String complianceNote() {
            return "跨境订单：需报关清单，不适用国内增值税发票";
        }
    }
}
