package com.allst.springboot.domain.order;

import com.allst.springboot.annotation.BatchLimited;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class OrderBatchCreateCommand {

    @NotEmpty(message = "批量数据不能为空")
    @BatchLimited
    @Valid
    private List<OrderCreateCommand> orders;
}
