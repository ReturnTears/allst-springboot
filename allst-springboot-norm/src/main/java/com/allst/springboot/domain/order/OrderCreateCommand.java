package com.allst.springboot.domain.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@Schema(description = "创建订单")
public class OrderCreateCommand {

    @NotNull(message = "用户编号不能为空")
    @Positive(message = "用户编号必须为正数")
    private Long userId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    @Digits(integer = 10, fraction = 2, message = "金额精度不正确")
    @Schema(example = "19.90")
    private BigDecimal amount;

    @Size(max = 200, message = "备注长度超出限制")
    private String remark;
}
