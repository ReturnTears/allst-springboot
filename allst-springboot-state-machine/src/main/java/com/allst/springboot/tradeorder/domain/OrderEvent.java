package com.allst.springboot.tradeorder.domain;

public enum OrderEvent {

    SUBMIT_PAY("发起支付"),
    PAY_SUCCESS("支付成功"),
    PAY_FAIL("支付失败"),
    CANCEL("取消订单"),
    TIMEOUT("超时关闭"),
    RISK_PASS("风控通过"),
    RISK_REJECT("风控拒绝"),
    ALLOCATE("占用库存"),
    START_REFUND("发起退款"),
    SHIP("发货"),
    SIGN("签收"),
    CONFIRM("确认完成"),
    APPLY_RETURN("申请退货"),
    APPROVE_RETURN("同意退货"),
    REJECT_RETURN("拒绝退货"),
    CLOSE_AFTER_SALE("关闭售后"),
    REFUND_SUCCESS("退款成功");

    private final String label;

    OrderEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
