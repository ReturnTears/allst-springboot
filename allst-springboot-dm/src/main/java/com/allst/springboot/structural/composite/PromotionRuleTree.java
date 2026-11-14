package com.allst.springboot.structural.composite;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 组合（Composite）——把「单个条件」和「条件组」当成同一种东西。
 *
 * 场景：优惠券可用规则是棵树：「满 100 且（会员 或 新人）且 非跨境单」。
 * 如果按过程写，每加一层括号就要改一次 if 嵌套；组合模式让叶子和组合节点
 * 实现同一个 RuleNode 接口，调用方永远只写 node.evaluate(ctx)。
 *
 * 对应开源项目里常见的商品类目树、组织权限树、规则树。真正复杂的规则应交给
 * 规则引擎或 SpEL（见 behavioral.interpreter），组合模式适合层级结构本身。
 */
public final class PromotionRuleTree {

    private PromotionRuleTree() {
    }

    public interface RuleNode {
        boolean evaluate(EvalContext ctx);

        String describe();
    }

    @Getter
    @Setter
    public static class EvalContext {
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean vip;
        private boolean newUser;
        private String channel = "DOMESTIC";
        private int riskScore;
        private int quantity = 1;
    }

    public static RuleNode and(RuleNode... children) {
        return new CompositeNode("AND", Arrays.asList(children), true);
    }

    public static RuleNode or(RuleNode... children) {
        return new CompositeNode("OR", Arrays.asList(children), false);
    }

    public static RuleNode not(RuleNode child) {
        return new NotNode(child);
    }

    public static RuleNode condition(String name, Predicate<EvalContext> predicate) {
        return new LeafNode(name, predicate);
    }

    /** 一条演示用规则：满 100 且（VIP 或 新人）且非跨境且风控分低于 80 */
    public static RuleNode couponGate() {
        return and(
                condition("金额>=100", c -> c.getAmount().compareTo(new BigDecimal("100")) >= 0),
                or(condition("VIP", EvalContext::isVip), condition("新人", EvalContext::isNewUser)),
                not(condition("跨境单", c -> "CROSS_BORDER".equals(c.getChannel()))),
                condition("风控通过", c -> c.getRiskScore() < 80)
        );
    }

    private static final class CompositeNode implements RuleNode {
        private final String operator;
        private final List<RuleNode> children;
        private final boolean allMatch;

        private CompositeNode(String operator, List<RuleNode> children, boolean allMatch) {
            this.operator = operator;
            this.children = List.copyOf(children);
            this.allMatch = allMatch;
        }

        @Override
        public boolean evaluate(EvalContext ctx) {
            if (children.isEmpty()) {
                return true;
            }
            return allMatch
                    ? children.stream().allMatch(n -> n.evaluate(ctx))
                    : children.stream().anyMatch(n -> n.evaluate(ctx));
        }

        @Override
        public String describe() {
            return operator + "(" + children.stream().map(RuleNode::describe).collect(Collectors.joining(", ")) + ")";
        }
    }

    private static final class NotNode implements RuleNode {
        private final RuleNode child;

        private NotNode(RuleNode child) {
            this.child = child;
        }

        @Override
        public boolean evaluate(EvalContext ctx) {
            return !child.evaluate(ctx);
        }

        @Override
        public String describe() {
            return "NOT(" + child.describe() + ")";
        }
    }

    private static final class LeafNode implements RuleNode {
        private final String name;
        private final Predicate<EvalContext> predicate;

        private LeafNode(String name, Predicate<EvalContext> predicate) {
            this.name = name;
            this.predicate = predicate;
        }

        @Override
        public boolean evaluate(EvalContext ctx) {
            return predicate.test(ctx);
        }

        @Override
        public String describe() {
            return name;
        }
    }
}
