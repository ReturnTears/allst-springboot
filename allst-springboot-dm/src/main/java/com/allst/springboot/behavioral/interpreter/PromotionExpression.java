package com.allst.springboot.behavioral.interpreter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 解释器（Interpreter）——把「满 100 且（VIP 或 新人）」这类规则变成可求值对象。
 * <Pre>
 * 场景：运营要能配置促销门槛，而不是每次改规则都发版。定义一种小语言后，
 * 每个语法节点都是一个表达式对象，组合出语法树，对同一个上下文求值。
 * <Pre>
 * 这里给两种落地：
 *  1) 手写 AST：TerminalExpression（金额、VIP）、NonTerminalExpression（AND/OR/NOT），
 *     用来讲清模式结构；
 *  2) SpEL：Spring 自带的解释器，evaluate() 一行就够。
 *
 * 生产建议：表达式一多就别自己写解析器，直接用 SpEL / Aviator / Drools。
 * 自己维护词法、语法、优先级和类型 conversion 的成本远高于引入成熟引擎。
 */
public final class PromotionExpression {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private PromotionExpression() {
    }

    @Getter
    @Setter
    public static class Context {
        private BigDecimal amount = BigDecimal.ZERO;
        private boolean vip;
        private boolean newUser;
        private int riskScore;
        private String channel = "DOMESTIC";
        private int quantity = 1;
    }

    public interface Expr {
        boolean interpret(Context ctx);
    }

    public static Expr amountAtLeast(BigDecimal threshold) {
        return ctx -> ctx.getAmount().compareTo(threshold) >= 0;
    }

    public static Expr vip() {
        return Context::isVip;
    }

    public static Expr newUser() {
        return Context::isNewUser;
    }

    public static Expr riskBelow(int score) {
        return ctx -> ctx.getRiskScore() < score;
    }

    public static Expr and(Expr... children) {
        List<Expr> list = Arrays.asList(children);
        return ctx -> list.stream().allMatch(e -> e.interpret(ctx));
    }

    public static Expr or(Expr... children) {
        List<Expr> list = Arrays.asList(children);
        return ctx -> list.stream().anyMatch(e -> e.interpret(ctx));
    }

    public static Expr not(Expr child) {
        return ctx -> !child.interpret(ctx);
    }

    /** 手写语法树：满 100 且（VIP 或 新人）且风控通过 */
    public static Expr demoAst() {
        return and(amountAtLeast(new BigDecimal("100")),
                or(vip(), newUser()),
                riskBelow(80));
    }

    /** Spring 的解释器：表达式是字符串，属性名对应 Context 的 getter */
    public static boolean evaluate(String expression, Context ctx) {
        StandardEvaluationContext context = new StandardEvaluationContext(ctx);
        Boolean value = PARSER.parseExpression(expression).getValue(context, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    public static final class Demo {
        public static final String SPEL = "amount >= 100 and (vip or newUser) and riskScore < 80";

        private Demo() {
        }
    }
}
