package com.allst.springboot.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 创建用户的入参。校验失败直接拒绝，不进入业务和数据库。
 * 对应规范：5 参数校验。
 */
@Data
@Schema(description = "创建用户")
public class UserCreateCommand {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名长度超出限制")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名需为 4 到 32 位字母、数字或下划线")
    @Schema(example = "zhangsan")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度超出限制")
    @Schema(example = "张三")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(example = "13800138000")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度超出限制")
    @Schema(example = "zhangsan@example.com")
    private String email;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    @Schema(example = "110101199001011234")
    private String idCard;
}
