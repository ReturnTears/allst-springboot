package com.allst.springboot.domain.user;

import com.allst.springboot.annotation.BatchLimited;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(description = "批量创建用户")
public class UserBatchCreateCommand {

    @NotEmpty(message = "批量数据不能为空")
    @BatchLimited
    @Valid
    private List<UserCreateCommand> users;
}
