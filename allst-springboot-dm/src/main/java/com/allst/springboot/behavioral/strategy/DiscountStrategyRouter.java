package com.allst.springboot.behavioral.strategy;

import com.allst.springboot.creational.singleton.RuleRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略（Strategy）——同一笔订单按会员/满减/新人券算不同的价。
 * <Pre>
 * 场景：促销规则每周都变，价格计算里如果堆 if (vip) ... else if (coupon) ...，
 * 每次上新活动都要改主流程，也难以单独测试某条规则。
 * 策略把「怎么算」封进各自的实现，路由只负责选一个。
 * <Pre>
 * Spring 里最实用的写法：每个策略是一个 @Component，并约定 type()；
 * 服务注入 Map&lt;String, DiscountStrategy&gt; 或 List 后按 type 建索引，
 * 新增策略只加一个 bean，路由代码一行不改。本例策略实现收在类内部，
 * 是为了让四种算法能在一屏里对着看，索引/查找方式与注入版一致。
 * <Pre>
 * 阈值不写死在读单例规则缓存 RuleRegistry，改配置不用改算法。
 */
@Component
public class DiscountStrategyRouter {

    public interface DiscountStrategy {
        String type();

        BigDecimal discount(DiscountContext ctx);
    }

    @Getter
    @Setter
    public static class DiscountContext {
        private BigDecimal originalAmount = BigDecimal.ZERO;
        private boolean vip;
        private boolean newUser;
        private String couponCode;
        private String channel = "DOMESTIC";
    }

    private final Map<String, DiscountStrategy> strategies = new LinkedHashMap<>();
    private final RuleRegistry rules;

    public DiscountStrategyRouter(RuleRegistry rules) {
        this.rules = rules;
        register(new NoDiscount());
        register(new FullReduction(rules));
        register(new VipDiscount(rules));
        register(new NewUserCoupon());
    }

    public BigDecimal apply(String type, DiscountContext ctx) {
        DiscountStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("未知优惠策略: " + type);
        }
        return cap(strategy.discount(ctx), ctx.getOriginalAmount());
    }

    /** 按「这次能用的最大优惠」自动选策略，等价于规则引擎里挑最优命中 */
    public String bestType(DiscountContext ctx) {
        String best = "NONE";
        BigDecimal max = BigDecimal.ZERO;
        for (Map.Entry<String, DiscountStrategy> entry : strategies.entrySet()) {
            BigDecimal value = cap(entry.getValue().discount(ctx), ctx.getOriginalAmount());
            if (value.compareTo(max) > 0) {
                max = value;
                best = entry.getKey();
            }
        }
        return best;
    }

    public List<String> types() {
        return Collections.unmodifiableList(new ArrayList<>(strategies.keySet()));
    }

    private void register(DiscountStrategy strategy) {
        strategies.put(strategy.type(), strategy);
    }

    private static BigDecimal cap(BigDecimal discount, BigDecimal amount) {
        if (discount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return discount.min(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class NoDiscount implements DiscountStrategy {
        @Override
        public String type() {
            return "NONE";
        }

        @Override
        public BigDecimal discount(DiscountContext ctx) {
            return BigDecimal.ZERO;
        }
    }

    private static final class FullReduction implements DiscountStrategy {
        private final RuleRegistry rules;

        private FullReduction(RuleRegistry rules) {
            this.rules = rules;
        }

        @Override
        public String type() {
            return "FULL_REDUCTION";
        }

        @Override
        public BigDecimal discount(DiscountContext ctx) {
            return ctx.getOriginalAmount().compareTo(rules.get("FULL_REDUCTION_THRESHOLD")) >= 0
                    ? rules.get("FULL_REDUCTION_AMOUNT")
                    : BigDecimal.ZERO;
        }
    }

    private static final class VipDiscount implements DiscountStrategy {
        private final RuleRegistry rules;

        private VipDiscount(RuleRegistry rules) {
            this.rules = rules;
        }

        @Override
        public String type() {
            return "VIP";
        }

        @Override
        public BigDecimal discount(DiscountContext ctx) {
            return ctx.isVip()
                    ? ctx.getOriginalAmount().multiply(BigDecimal.ONE.subtract(rules.get("VIP_DISCOUNT")))
                    : BigDecimal.ZERO;
        }
    }

    private static final class NewUserCoupon implements DiscountStrategy {
        @Override
        public String type() {
            return "NEW_USER";
        }

        @Override
        public BigDecimal discount(DiscountContext ctx) {
            return ctx.isNewUser() && "NEW10".equals(ctx.getCouponCode())
                    ? new BigDecimal("15")
                    : BigDecimal.ZERO;
        }
    }
}
