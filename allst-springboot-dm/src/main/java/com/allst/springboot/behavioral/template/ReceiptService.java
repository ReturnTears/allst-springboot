package com.allst.springboot.behavioral.template;

import com.allst.springboot.behavioral.template.ReceiptTemplate.Context;
import com.allst.springboot.behavioral.template.ReceiptTemplate.Receipt;
import com.allst.springboot.common.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 两个具体模板 + 一个按渠道选择的入口。
 */
@Component
public class ReceiptService {

    private final Map<String, ReceiptTemplate> templates = new LinkedHashMap<>();

    public ReceiptService() {
        templates.put("DOMESTIC", new DomesticReceipt());
        templates.put("CROSS_BORDER", new CrossBorderReceipt());
    }

    public Receipt generate(String channel, Context ctx) {
        ctx.setChannel(channel);
        ReceiptTemplate template = templates.get(channel);
        if (template == null) {
            throw new BizException("未知回执渠道: " + channel);
        }
        return template.generate(ctx);
    }

    public List<String> channels() {
        return List.copyOf(templates.keySet());
    }

    private static final class DomesticReceipt extends ReceiptTemplate {
        @Override
        protected List<String> loadLines(Context ctx) {
            return Arrays.asList("订单号 " + ctx.getOrderNo(), "购买人 " + ctx.getBuyer(),
                    "金额 " + ctx.getAmount().toPlainString());
        }

        @Override
        protected BigDecimal calculateTax(Context ctx) {
            return ctx.getAmount().multiply(new BigDecimal("0.13")).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static final class CrossBorderReceipt extends ReceiptTemplate {
        @Override
        protected List<String> loadLines(Context ctx) {
            return Arrays.asList("Order " + ctx.getOrderNo(), "Buyer " + ctx.getBuyer(),
                    "Amount " + ctx.getAmount().toPlainString());
        }

        @Override
        protected BigDecimal calculateTax(Context ctx) {
            return ctx.getAmount().multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        }

        @Override
        protected void decorate(Receipt receipt, Context ctx) {
            receipt.getTrail().add("附加报关清单");
        }
    }
}
