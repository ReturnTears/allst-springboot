package com.allst.springboot.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 只修改启用状态。和资料修改分开，避免一个接口同时改两类数据。
 */
@Data
@Schema(description = "修改用户状态")
public class UserStatusCommand {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    @Schema(description = "1 启用，0 停用", example = "0")
    private Integer status;
}
