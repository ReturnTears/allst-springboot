package com.allst.springboot.domain.user;

import com.allst.springboot.annotation.MaxPageSize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Data
@Schema(description = "用户分页条件")
public class UserPageQuery {

    @Min(value = 0, message = "页码不能小于 0")
    @Schema(description = "从 0 开始", example = "0")
    private int page = 0;

    @MaxPageSize
    @Schema(description = "每页条数，不能超过配置上限", example = "10")
    private int size = 10;

    @Size(max = 32, message = "用户名长度超出限制")
    @Schema(description = "用户名模糊条件，可空")
    private String username;
}
