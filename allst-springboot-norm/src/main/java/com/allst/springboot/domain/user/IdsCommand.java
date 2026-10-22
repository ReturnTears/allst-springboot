package com.allst.springboot.domain.user;

import com.allst.springboot.annotation.BatchLimited;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
@Schema(description = "批量编号")
public class IdsCommand {

    @NotEmpty(message = "编号列表不能为空")
    @BatchLimited
    private List<@NotNull(message = "编号不能为空") @Positive(message = "编号必须为正数") Long> ids;
}
