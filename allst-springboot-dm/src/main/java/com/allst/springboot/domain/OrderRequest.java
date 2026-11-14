package com.allst.springboot.domain;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 下单请求。一条主线里所有模式都围着它转，字段刻意覆盖真实业务里的几类分歧：
 * 渠道、支付方式、会员身份、优惠券、幂等号、风控分。
 */
@Getter
@Setter
public class OrderRequest {

    @NotBlank(message = "买家不能为空")
    private String buyer;

    @NotBlank(message = "SKU 不能为空")
    private String sku;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于 0")
    private BigDecimal unitPrice;

    @Min(value = 1, message = "数量至少为 1")
    private int quantity = 1;

    /** 会员身份影响折扣策略 */
    private boolean vip;

    /** 优惠券码，进解释器表达式与规则树 */
    private String couponCode;

    /** ALIPAY / WECHAT / BALANCE，决定工厂方法创建哪个支付命令 */
    @NotBlank(message = "支付方式不能为空")
    private String payChannel;

    /** DOMESTIC / CROSS_BORDER，决定抽象工厂选哪一族组件 */
    private String channel = "DOMESTIC";

    /** 幂等键，重复提交同一 requestId 不应产生两笔订单 */
    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    /** 风控分，责任链里的一个环节 */
    private int riskScore;

    public BigDecimal originalAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
