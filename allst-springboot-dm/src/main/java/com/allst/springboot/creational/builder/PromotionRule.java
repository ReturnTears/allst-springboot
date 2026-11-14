package com.allst.springboot.creational.builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 建造者（Builder）——一条促销规则有十来个可选字段，且字段之间有约束。
 *
 * 场景：运营后台配置「满减 / 折扣 / 新人券」规则。用全参构造器会出现
 * new PromotionRule(..., null, null, true, ...) 这种没人看得懂的调用；
 * 用 setter 又会产生「还差必填项就被发布出去」的半成品对象。
 *
 * Builder 把校验收在 build() 里，产出不可变对象，可以直接放进缓存或发给下游。
 * 什么时候不用：只有两三个字段、且没有组合约束时，构造器或 Lombok @Builder 就够了。
 */
public final class PromotionRule {

    public enum Type {
        FULL_REDUCTION, PERCENT, NEW_USER
    }

    private final String code;
    private final String name;
    private final Type type;
    private final BigDecimal threshold;
    private final BigDecimal value;
    private final Set<String> channels;
    private final Set<String> excludedTags;
    private final int priority;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final String expression;

    private PromotionRule(Builder b) {
        this.code = b.code;
        this.name = b.name;
        this.type = b.type;
        this.threshold = b.threshold;
        this.value = b.value;
        this.channels = Collections.unmodifiableSet(new LinkedHashSet<>(b.channels));
        this.excludedTags = Collections.unmodifiableSet(new LinkedHashSet<>(b.excludedTags));
        this.priority = b.priority;
        this.validFrom = b.validFrom;
        this.validTo = b.validTo;
        this.expression = b.expression;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Set<String> getChannels() {
        return channels;
    }

    public Set<String> getExcludedTags() {
        return excludedTags;
    }

    public int getPriority() {
        return priority;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getExpression() {
        return expression;
    }

    public boolean effectiveOn(LocalDate day) {
        return !day.isBefore(validFrom) && !day.isAfter(validTo);
    }

    public boolean allowsChannel(String channel) {
        return channels.isEmpty() || channels.contains(channel);
    }

    @Override
    public String toString() {
        return code + "(" + name + "," + type + ",priority=" + priority + ")";
    }

    public static final class Builder {
        private String code;
        private String name;
        private Type type = Type.FULL_REDUCTION;
        private BigDecimal threshold = BigDecimal.ZERO;
        private BigDecimal value = BigDecimal.ZERO;
        private final Set<String> channels = new LinkedHashSet<>();
        private final Set<String> excludedTags = new LinkedHashSet<>();
        private int priority = 100;
        private LocalDate validFrom = LocalDate.of(2020, 1, 1);
        private LocalDate validTo = LocalDate.of(2099, 12, 31);
        private String expression;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder threshold(BigDecimal threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder value(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder channel(String channel) {
            this.channels.add(channel);
            return this;
        }

        public Builder excludeTag(String tag) {
            this.excludedTags.add(tag);
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder validBetween(LocalDate from, LocalDate to) {
            this.validFrom = from;
            this.validTo = to;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public PromotionRule build() {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("规则编码必填");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("规则名称必填");
            }
            if (threshold.signum() < 0) {
                throw new IllegalArgumentException("门槛不能为负");
            }
            if (type == Type.FULL_REDUCTION && value.signum() <= 0) {
                throw new IllegalArgumentException("满减规则必须给出减免金额");
            }
            if (type == Type.PERCENT
                    && (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.ONE) > 0)) {
                throw new IllegalArgumentException("折扣系数必须在 (0, 1] 之间");
            }
            if (validTo.isBefore(validFrom)) {
                throw new IllegalArgumentException("生效结束时间早于开始时间");
            }
            return new PromotionRule(this);
        }
    }
}
