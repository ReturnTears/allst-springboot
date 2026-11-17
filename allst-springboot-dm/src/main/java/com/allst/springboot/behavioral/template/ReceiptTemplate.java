package com.allst.springboot.behavioral.template;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模板方法（Template Method）——回执/对账单的生成骨架只写一份。
 * <Pre>
 * 场景：国内回执和跨境回执的步骤一样（校验 → 取明细 → 算税 → 排版 → 落痕），
 * 差别只在明细字段和计税方式。两个任务各自复制骨架，改一处校验就漏一处。
 * 基类用 final 固定步骤顺序，子类只填抽象步骤和可选钩子（hook）。
 * <Pre>
 * Spring 里的 JdbcTemplate、RestTemplate、AbstractRoutingDataSource、
 * xxl-job 的 IJobHandler、批量导入的 AbstractImporter 都是这个套路。
 * 注意别把骨架方法写成可覆盖的，否则子类一不小心又复制一遍流程。
 */
public abstract class ReceiptTemplate {

    /** 固定流程，子类不能改顺序 */
    public final Receipt generate(Context ctx) {
        validate(ctx);
        List<String> lines = loadLines(ctx);
        BigDecimal tax = calculateTax(ctx);
        Receipt receipt = new Receipt(ctx.getOrderNo(), ctx.getChannel(), lines, tax, ctx.getAmount());
        decorate(receipt, ctx);
        record(receipt);
        return receipt;
    }

    protected void validate(Context ctx) {
        if (ctx.getOrderNo() == null || ctx.getOrderNo().isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (ctx.getAmount() == null || ctx.getAmount().signum() < 0) {
            throw new IllegalArgumentException("金额不合法");
        }
    }

    protected abstract List<String> loadLines(Context ctx);

    protected abstract BigDecimal calculateTax(Context ctx);

    /** 钩子：子类可以补内容，不补也没问题 */
    protected void decorate(Receipt receipt, Context ctx) {
        // 默认不做额外处理
    }

    private void record(Receipt receipt) {
        receipt.getTrail().add("生成完成: " + receipt.getChannel());
    }

    @Getter
    @Setter
    public static class Context {
        private String orderNo;
        private String channel = "DOMESTIC";
        private BigDecimal amount = BigDecimal.ZERO;
        private String buyer;
    }

    @Getter
    public static final class Receipt {
        private final String orderNo;
        private final String channel;
        private final List<String> lines;
        private final BigDecimal tax;
        private final BigDecimal amount;
        private final List<String> trail = new ArrayList<>();

        private Receipt(String orderNo, String channel, List<String> lines,
                        BigDecimal tax, BigDecimal amount) {
            this.orderNo = orderNo;
            this.channel = channel;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.tax = tax;
            this.amount = amount;
            this.trail.add("校验通过");
            this.trail.add("明细 " + lines.size() + " 行");
        }
    }
}
