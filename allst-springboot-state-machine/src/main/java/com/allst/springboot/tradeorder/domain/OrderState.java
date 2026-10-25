package com.allst.springboot.tradeorder.domain;

/**
 * 订单状态。带 structural 标记的是状态机内部的分流或父状态，业务上不会停在那里。
 */
public enum OrderState {

    CREATED("待支付"),
    PAYING("支付中"),
    PAY_FAILED("支付失败"),
    PAY_ROUTE("支付分流", true),
    RISK_REVIEW("风控审核"),
    RISK_REJECTED("风控拒绝"),
    WAIT_ALLOCATE("待占库存"),
    ALLOCATE_ROUTE("库存分流", true),
    STOCK_SHORT("缺货"),
    WAIT_SHIP("待发货"),
    SHIPPED("已发货"),
    DELIVERED("已签收"),
    AFTER_SALE("售后中", true),
    RETURN_APPLIED("退货已申请"),
    RETURN_APPROVED("退货已同意"),
    RETURN_REJECTED("退货已拒绝"),
    REFUNDING("退款中"),
    REFUNDED("已退款"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    CLOSED("已关闭");

    private final String label;
    private final boolean structural;

    OrderState(String label) {
        this(label, false);
    }

    OrderState(String label, boolean structural) {
        this.label = label;
        this.structural = structural;
    }

    public String getLabel() {
        return label;
    }

    public boolean isStructural() {
        return structural;
    }
}
