package com.allst.springboot.behavioral.visitor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 访问者（Visitor）——对不同类型的订单行做不同的计算，但不把判断塞进实体。
 * <Pre>
 * 场景：订单行有实物、虚拟商品、礼品卡。要分别算「应收金额」「发票品类」「风控标记」，
 * 如果在每个实体里都加 computeAmount()/invoiceCategory()/riskTag()，加一种计算就要改所有类；
 * 如果在外层写 instanceof 链，新增一种商品类型又要找遍所有计算。
 * 访问者用双分派：元素 accept(visitor)，由元素自己决定调用哪个 visit 方法，
 * 新增一种计算只需新增一个 Visitor。
 * <Pre>
 * 代价：新增一种元素类型要改所有 Visitor 的接口。所以适合「元素类型稳定、
 * 操作经常增加」的场景，例如报表、导出、发票、风控多套口径。
 */
public final class OrderLineVisitorDemo {

    private OrderLineVisitorDemo() {
    }

    public interface OrderItem {
        <R> R accept(Visitor<R> visitor);

        String sku();

        BigDecimal unitPrice();

        int quantity();
    }

    public interface Visitor<R> {
        R visitPhysical(PhysicalItem item);

        R visitVirtual(VirtualItem item);

        R visitGiftCard(GiftCardItem item);
    }

    public abstract static class BaseItem implements OrderItem {
        private final String sku;
        private final BigDecimal unitPrice;
        private final int quantity;

        protected BaseItem(String sku, BigDecimal unitPrice, int quantity) {
            this.sku = sku;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        @Override
        public String sku() {
            return sku;
        }

        @Override
        public BigDecimal unitPrice() {
            return unitPrice;
        }

        @Override
        public int quantity() {
            return quantity;
        }
    }

    public static final class PhysicalItem extends BaseItem {
        public PhysicalItem(String sku, BigDecimal unitPrice, int quantity) {
            super(sku, unitPrice, quantity);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPhysical(this);
        }
    }

    public static final class VirtualItem extends BaseItem {
        public VirtualItem(String sku, BigDecimal unitPrice, int quantity) {
            super(sku, unitPrice, quantity);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitVirtual(this);
        }
    }

    public static final class GiftCardItem extends BaseItem {
        public GiftCardItem(String sku, BigDecimal unitPrice, int quantity) {
            super(sku, unitPrice, quantity);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGiftCard(this);
        }
    }

    /** 口径一：应收金额（礼品卡不计入实物营收） */
    public static final class AmountVisitor implements Visitor<BigDecimal> {
        @Override
        public BigDecimal visitPhysical(PhysicalItem item) {
            return item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        }

        @Override
        public BigDecimal visitVirtual(VirtualItem item) {
            return item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        }

        @Override
        public BigDecimal visitGiftCard(GiftCardItem item) {
            return BigDecimal.ZERO;
        }
    }

    /** 口径二：发票品类 */
    public static final class InvoiceVisitor implements Visitor<String> {
        @Override
        public String visitPhysical(PhysicalItem item) {
            return "货物:" + item.sku();
        }

        @Override
        public String visitVirtual(VirtualItem item) {
            return "信息服务:" + item.sku();
        }

        @Override
        public String visitGiftCard(GiftCardItem item) {
            return "不征税(预付卡):" + item.sku();
        }
    }

    public static List<OrderItem> sampleItems() {
        return Arrays.asList(
                new PhysicalItem("SKU-PHONE", new BigDecimal("2999"), 1),
                new VirtualItem("SKU-CLOUD", new BigDecimal("199"), 2),
                new GiftCardItem("SKU-GIFTCARD", new BigDecimal("500"), 1));
    }

    public static Map<String, Object> summarize(List<OrderItem> items) {
        AmountVisitor amount = new AmountVisitor();
        InvoiceVisitor invoice = new InvoiceVisitor();
        BigDecimal total = BigDecimal.ZERO;
        List<String> categories = new java.util.ArrayList<>();
        for (OrderItem item : items) {
            total = total.add(item.accept(amount));
            categories.add(item.accept(invoice));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("recognisedAmount", total);
        summary.put("invoiceCategories", categories);
        return summary;
    }
}
