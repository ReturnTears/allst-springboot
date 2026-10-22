package com.allst.springboot.domain.order;

import com.allst.springboot.annotation.MaxPageSize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@Data
@Schema(description = "订单分页条件")
public class OrderPageQuery {

    @Min(value = 0, message = "页码不能小于 0")
    private int page = 0;

    @MaxPageSize
    private int size = 10;

    @Positive(message = "用户编号必须为正数")
    @Schema(description = "按用户过滤，可空")
    private Long userId;
}
